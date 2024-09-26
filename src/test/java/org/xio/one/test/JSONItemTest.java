package org.xio.one.test;

import org.junit.Test;

public class JSONItemTest {
  @Test
  public void test() {
    JSONUtil.toJSONString(new ItemToJSONPojo<>());
  }

}
