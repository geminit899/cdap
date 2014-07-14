/*
 * Copyright 2012-2014 Continuuity, Inc.
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

package com.continuuity.common.collect;

import org.junit.Assert;
import org.junit.Test;

public class CollectorTest {


  private Integer[] collect(Collector<Integer> collector, int n) {
    for (int i = 0; i < n; i++) {
      if (!collector.addElement(i)) {
        break;
      }
    }
    return collector.finish();
  }

  @Test
  public void testAllCollector() {
    Assert.assertArrayEquals(collect(new AllCollector<Integer>(Integer.class), 0), new Integer[] { });
    Assert.assertArrayEquals(collect(new AllCollector<Integer>(Integer.class), 4), new Integer[] { 0, 1, 2, 3 });
  }

  @Test
  public void testFirstNCollector() {
    // add 0 elements
    Assert.assertArrayEquals(collect(new FirstNCollector<Integer>(1, Integer.class), 0), new Integer[] { });
    Assert.assertArrayEquals(collect(new FirstNCollector<Integer>(10, Integer.class), 0), new Integer[] { });
    // add more than capacity
    Assert.assertArrayEquals(collect(new FirstNCollector<Integer>(1, Integer.class), 10), new Integer[] { 0 });
    Assert.assertArrayEquals(collect(new FirstNCollector<Integer>(4, Integer.class), 10), new Integer[] { 0, 1, 2, 3 });
    // add same as capacity
    Assert.assertArrayEquals(collect(new FirstNCollector<Integer>(1, Integer.class), 1), new Integer[] { 0 });
    Assert.assertArrayEquals(collect(new FirstNCollector<Integer>(4, Integer.class), 4), new Integer[] { 0, 1, 2, 3 });
    // add less than capacity
    Assert.assertArrayEquals(collect(new FirstNCollector<Integer>(4, Integer.class), 1), new Integer[] { 0 });
    Assert.assertArrayEquals(collect(new FirstNCollector<Integer>(10, Integer.class), 4), new Integer[] { 0, 1, 2, 3 });
  }

  @Test
  public void testLastNCollector() {
    // add 0 elements
    Assert.assertArrayEquals(collect(new LastNCollector<Integer>(1, Integer.class), 0), new Integer[] { });
    Assert.assertArrayEquals(collect(new LastNCollector<Integer>(10, Integer.class), 0), new Integer[] { });
    // add more than capacity
    Assert.assertArrayEquals(collect(new LastNCollector<Integer>(1, Integer.class), 10), new Integer[] { 9 });
    Assert.assertArrayEquals(collect(new LastNCollector<Integer>(4, Integer.class), 10), new Integer[] { 6, 7, 8, 9 });
    // add same as capacity
    Assert.assertArrayEquals(collect(new LastNCollector<Integer>(1, Integer.class), 1), new Integer[] { 0 });
    Assert.assertArrayEquals(collect(new LastNCollector<Integer>(4, Integer.class), 4), new Integer[] { 0, 1, 2, 3 });
    // add less than capacity
    Assert.assertArrayEquals(collect(new LastNCollector<Integer>(4, Integer.class), 1), new Integer[] { 0 });
    Assert.assertArrayEquals(collect(new LastNCollector<Integer>(10, Integer.class), 4), new Integer[] { 0, 1, 2, 3 });
  }

}
