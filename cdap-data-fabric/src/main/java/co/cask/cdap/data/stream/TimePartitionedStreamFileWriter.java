/*
 * Copyright © 2014-2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package co.cask.cdap.data.stream;

import co.cask.cdap.api.flow.flowlet.StreamEvent;
import co.cask.cdap.common.security.Impersonator;
import co.cask.cdap.data.file.FileWriter;
import co.cask.cdap.data.file.PartitionedFileWriter;
import co.cask.cdap.data.stream.TimePartitionedStreamFileWriter.TimePartition;
import co.cask.cdap.proto.id.StreamId;
import com.google.common.base.Throwables;
import com.google.common.io.OutputSupplier;
import com.google.common.primitives.Longs;
import org.apache.twill.filesystem.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Stream file path format:
 *
 * <br/><br/>
 *   Each file has path pattern
 * <pre>
 *     [streamName]/[partitionName]/[bucketName].[dat|idx]
 * </pre>
 * Where {@code .dat} is the event data file, {@code .idx} is the accompany index file.
 *
 * <br/><br/>
 * The {@code partitionName} is formatted as
 * <pre>
 *   [partitionStartTime].[duration]
 * </pre>
 * with both {@code partitionStartTime} and {@code duration} in seconds.
 *
 * <br/><br/>
 * The {@code bucketName} is formatted as
 * <pre>
 *   "bucket".[bucketId].[seqNo]
 * </pre>
 * where the {@code bucketId} is an integer. The {@code seqNo} is a strictly increasing integer for the same
 * {@code bucketId}.
 */
@NotThreadSafe
public class TimePartitionedStreamFileWriter extends PartitionedFileWriter<StreamEvent, TimePartition> {

  private static final Logger LOG = LoggerFactory.getLogger(TimePartitionedStreamFileWriter.class);

  private final long partitionDuration;
  private TimePartition timePartition = new TimePartition(-1L);

  public TimePartitionedStreamFileWriter(Location streamLocation, long partitionDuration,
                                         String fileNamePrefix, long indexInterval, StreamId streamId,
                                         Impersonator impersonator) {
    super(new StreamWriterFactory(streamLocation, partitionDuration, fileNamePrefix, indexInterval),
          streamId, impersonator);
    this.partitionDuration = partitionDuration;
  }

  @Override
  protected TimePartition getPartition(StreamEvent event) {
    long eventPartitionStart = StreamUtils.getPartitionStartTime(event.getTimestamp(), partitionDuration);
    if (eventPartitionStart != timePartition.getStartTimestamp()) {
      timePartition = new TimePartition(eventPartitionStart);
    }
    return timePartition;
  }

  @Override
  protected void partitionChanged(TimePartition oldPartition, TimePartition newPartition) throws IOException {
    closePartitionWriter(oldPartition);
  }

  /**
   * Uses timestamp to represent partition information.
   */
  public static final class TimePartition {

    private final long startTimestamp;

    private TimePartition(long startTimestamp) {
      this.startTimestamp = startTimestamp;
    }

    private long getStartTimestamp() {
      return startTimestamp;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TimePartition other = (TimePartition) o;
      return startTimestamp == other.startTimestamp;
    }

    @Override
    public int hashCode() {
      return Longs.hashCode(startTimestamp);
    }
  }


  private static final class StreamWriterFactory implements PartitionedFileWriterFactory<StreamEvent, TimePartition> {

    private final Location streamLocation;
    private final long partitionDuration;
    private final String fileNamePrefix;
    private final long indexInterval;
    private final Timer timer;

    StreamWriterFactory(Location streamLocation, long partitionDuration, String fileNamePrefix, long indexInterval) {
      this.streamLocation = streamLocation;
      this.partitionDuration = partitionDuration;
      this.fileNamePrefix = fileNamePrefix;
      this.indexInterval = indexInterval;
      this.timer = new Timer();
    }

    @Override
    public FileWriter<StreamEvent> create(TimePartition partition) throws IOException {
      long partitionStart = partition.getStartTimestamp();

      if (!streamLocation.isDirectory()) {
        throw new IOException("Stream " + streamLocation.getName() + " not exist in " + streamLocation);
      }

      Location partitionDirectory = StreamUtils.createPartitionLocation(streamLocation,
                                                                        partitionStart, partitionDuration);
      // Always try to create the directory
      partitionDirectory.mkdirs();

      // Create the event and index file with the next sequence id
      int fileSequence = StreamUtils.getNextSequenceId(partitionDirectory, fileNamePrefix);
      Location eventFile = StreamUtils.createStreamLocation(partitionDirectory, fileNamePrefix,
                                                            fileSequence, StreamFileType.EVENT);
      Location indexFile = StreamUtils.createStreamLocation(partitionDirectory, fileNamePrefix,
                                                            fileSequence, StreamFileType.INDEX);
      // The creation should succeed, as it's expected to only have one process running per fileNamePrefix.
      if (!eventFile.createNew() || !indexFile.createNew()) {
        throw new IOException("Failed to create new file at " + eventFile + " and " + indexFile);
      }

      LOG.debug("New stream file created at {}", eventFile);

      final StreamDataFileWriter streamDataFileWriter =
        new StreamDataFileWriter(createOutputSupplier(eventFile), createOutputSupplier(indexFile), indexInterval);

      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          try {
            // NOTE: this doesn't remove it from PartitionedFileWriter map of writers
            // it will get removed from there when the next event comes
            streamDataFileWriter.close();
          } catch (IOException e) {
            Throwables.propagate(e);
          }
        }
         // add a 5 minute buffer, to ensure that all events for this time partition are enqueued to the writer
         // before closing it
      }, partitionDuration + TimeUnit.MINUTES.toMillis(5));
      return streamDataFileWriter;
    }

    private OutputSupplier<OutputStream> createOutputSupplier(final Location location) {
      return new OutputSupplier<OutputStream>() {
        @Override
        public OutputStream getOutput() throws IOException {
          return location.getOutputStream();
        }
      };
    }
  }
}
