import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.http.weeio.event.platform.domain.request.FilterExpression;
import org.xio.one.reactive.http.weeio.event.platform.domain.selector.FilterEntry;
import org.xio.one.reactive.http.weeio.event.platform.domain.selector.FilterOperations;

public class FilterExpressionShould {
  @Test
  public void returnFilterEntryStringWhenStringWithQuotesIsPassed() {
    FilterExpression filterExpression = new FilterExpression("myfield", FilterOperations.EQ,"'hello mum'");
    FilterEntry filterEntry = filterExpression.filterEntry();
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.is(String.class));
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.equalTo("hello mum"));
    Assert.assertThat(filterEntry.getOperator(), CoreMatchers.equalTo(FilterOperations.EQ));
    Assert.assertThat(filterEntry.getField(), CoreMatchers.equalTo("myfield"));
  }

  @Test
  public void returnFilterEntryLongWhenLongStringIsPassed() {
    FilterExpression filterExpression = new FilterExpression("myfield", FilterOperations.GT,"3497823498724927349");
    FilterEntry filterEntry = filterExpression.filterEntry();
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.is(Long.class));
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.equalTo(3497823498724927349L));
    Assert.assertThat(filterEntry.getOperator(), CoreMatchers.equalTo(FilterOperations.GT));
    Assert.assertThat(filterEntry.getField(), CoreMatchers.equalTo("myfield"));
  }

  @Test
  public void returnFilterEntryIntegerWhenIntegerStringIsPassed() {
    FilterExpression filterExpression = new FilterExpression("myfield", FilterOperations.GT,"349782");
    FilterEntry filterEntry = filterExpression.filterEntry();
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.is(Integer.class));
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.equalTo(349782));
    Assert.assertThat(filterEntry.getOperator(), CoreMatchers.equalTo(FilterOperations.GT));
    Assert.assertThat(filterEntry.getField(), CoreMatchers.equalTo("myfield"));
  }

  @Test
  public void returnFilterEntryDoubleWhenDoubleStringIsPassed() {
    FilterExpression filterExpression = new FilterExpression("myfield", FilterOperations.LT,"349782.28729373");
    FilterEntry filterEntry = filterExpression.filterEntry();
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.is(Double.class));
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.equalTo(349782.28729373));
    Assert.assertThat(filterEntry.getOperator(), CoreMatchers.equalTo(FilterOperations.LT));
    Assert.assertThat(filterEntry.getField(), CoreMatchers.equalTo("myfield"));
  }

  @Test
  public void returnFilterEntryFloatWhenFloatStringIsPassed() {
    FilterExpression filterExpression = new FilterExpression("myfield", FilterOperations.LT,"3.497822872937338E23");
    FilterEntry filterEntry = filterExpression.filterEntry();
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.is(Double.class));
    Assert.assertThat(filterEntry.getValue(), CoreMatchers.equalTo(3.497822872937338E23));
    Assert.assertThat(filterEntry.getOperator(), CoreMatchers.equalTo(FilterOperations.LT));
    Assert.assertThat(filterEntry.getField(), CoreMatchers.equalTo("myfield"));
  }



}
