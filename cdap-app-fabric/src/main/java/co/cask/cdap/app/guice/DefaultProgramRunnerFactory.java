/*
 * Copyright © 2016 Cask Data, Inc.
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

package co.cask.cdap.app.guice;

import co.cask.cdap.app.program.Program;
import co.cask.cdap.app.runtime.ProgramClassLoaderProvider;
import co.cask.cdap.app.runtime.ProgramController;
import co.cask.cdap.app.runtime.ProgramOptions;
import co.cask.cdap.app.runtime.ProgramRunner;
import co.cask.cdap.app.runtime.ProgramRunnerFactory;
import co.cask.cdap.app.runtime.ProgramRuntimeProvider;
import co.cask.cdap.app.runtime.ProgramStateWriter;
import co.cask.cdap.internal.app.program.StateChangeListener;
import co.cask.cdap.internal.app.runtime.ProgramRuntimeProviderLoader;
import co.cask.cdap.proto.ProgramType;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import org.apache.twill.common.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * The default implementation of {@link ProgramRunnerFactory} used inside app-fabric.
 */
public final class DefaultProgramRunnerFactory implements ProgramRunnerFactory {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultProgramRunnerFactory.class);

  private final Injector injector;
  private final Map<ProgramType, Provider<ProgramRunner>> defaultRunnerProviders;
  private final ProgramRuntimeProvider.Mode mode;
  private final ProgramRuntimeProviderLoader runtimeProviderLoader;
  private final ProgramStateWriter programStateWriter;

  @Inject
  DefaultProgramRunnerFactory(Injector injector, ProgramRuntimeProvider.Mode mode,
                              ProgramRuntimeProviderLoader runtimeProviderLoader,
                              Map<ProgramType, Provider<ProgramRunner>> defaultRunnerProviders,
                              ProgramStateWriter programStateWriter) {
    this.injector = injector;
    this.defaultRunnerProviders = defaultRunnerProviders;
    this.mode = mode;
    this.runtimeProviderLoader = runtimeProviderLoader;
    this.programStateWriter = programStateWriter;
  }

  @Override
  public ProgramRunner create(ProgramType programType) {
    ProgramRuntimeProvider provider = runtimeProviderLoader.get(programType);
    ProgramRunner runner;

    if (provider != null) {
      LOG.debug("Using runtime provider {} for program type {}", provider, programType);
      runner = provider.createProgramRunner(programType, mode, injector);
    } else {
      Provider<ProgramRunner> defaultProvider = defaultRunnerProviders.get(programType);
      if (defaultProvider == null) {
        throw new IllegalArgumentException("Unsupported program type: " + programType);
      }
      runner = defaultProvider.get();
    }

    // Wrap a state change listener around the controller returned by the program runner in standalone mode
    // In distributed mode, program state changes are recorded in the event handler by Twill AM.
    return (mode == ProgramRuntimeProvider.Mode.LOCAL)
      ? new LocalProgramRunner(runner, programStateWriter)
      : runner;
  }

  /**
   * A local program runner that wraps a state change listener around the program runner
   */
  private static final class LocalProgramRunner implements ProgramRunner, ProgramClassLoaderProvider, Closeable {

    private final ProgramRunner runner;
    private final ProgramStateWriter programStateWriter;

    private LocalProgramRunner(ProgramRunner runner, ProgramStateWriter programStateWriter) {
      this.runner = runner;
      this.programStateWriter = programStateWriter;
    }

    @Override
    public ProgramController run(Program program, ProgramOptions options) {
      return addStateChangeListener(runner.run(program, options), options);
    }

    private ProgramController addStateChangeListener(ProgramController controller, ProgramOptions options) {
      controller.addListener(new StateChangeListener(controller.getProgramRunId(), null, programStateWriter, options),
                             Threads.SAME_THREAD_EXECUTOR);
      return controller;
    }

    @Override
    @Nullable
    public ClassLoader createProgramClassLoaderParent() {
      if (runner instanceof ProgramClassLoaderProvider) {
        return ((ProgramClassLoaderProvider) runner).createProgramClassLoaderParent();
      }
      return null;
    }

    @Override
    public void close() throws IOException {
      if (runner instanceof Closeable) {
        ((Closeable) runner).close();
      }
    }
  }
}
