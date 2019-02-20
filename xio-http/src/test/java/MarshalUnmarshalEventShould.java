import org.junit.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.xio.one.reactive.http.weeio.internal.api.JSONUtil;
import org.xio.one.reactive.http.weeio.internal.domain.Event;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;

public class MarshalUnmarshalEventShould {

  @Test
  public void serializeTestBeanObjectToEventJSON() {
    TestBeanObject object = new TestBeanObject("hello");
    Event event = new Event(object);
    Assert.assertThat(event.toJSONString(), is("{\"testField\":\"hello\"}"));
  }

  @Test
  public void serializeTestBeanObjectToFullEventJSON() {
    TestBeanObject object = new TestBeanObject("hello");
    Event event = new Event(object);
    Assert
        .assertThat(event.toFullJSONString(), JUnitMatchers.containsString("\"_eventTimestamp\""));
    Assert.assertThat(event.toFullJSONString(), JUnitMatchers.containsString("\"_eventType\""));
    Assert.assertThat(event.toFullJSONString(), JUnitMatchers.containsString("\"_eventId\""));
    Assert.assertThat(event.toFullJSONString(), JUnitMatchers.containsString("\"_eventNodeId\""));
  }

  @Test
  public void canDeserializeFromJSON() throws IOException {
    TestBeanObject object = new TestBeanObject("hello");
    Event event = new Event(object);
    String jsonValue = event.toFullJSONString();
    Event event2 = JSONUtil.fromJSONString(jsonValue, Event.class);
    Assert.assertThat(event2.getFieldValue("testField"), is("hello"));
    Assert.assertTrue("Event Id : " + event2.get_eventId(), event2.get_eventId() > 0);
  }

  public class TestBeanObject {
    String testField;

    public TestBeanObject() {
    }

    public TestBeanObject(String testField) {
      this.testField = testField;
    }

    public String getTestField() {
      return testField;
    }
  }

}
