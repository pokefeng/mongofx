package mongofx.js.support;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class JsAntlrPathBuilderTest {

  @Test
  public void rootElementTest() {
    List<String> path = JsAntlrPathBuilder.buildPath("db", "db".length()).get();
    assertEquals(1, path.size());
    assertEquals("db", path.get(0));
  }

  @Test
  public void simpleMiddlePartTest() {
    List<String> path = JsAntlrPathBuilder.buildPath("db.get", "db.".length()).get();
    assertEquals(2, path.size());
    assertEquals("db", path.get(0));
    assertEquals("", path.get(1));
  }

  @Test
  public void simplePartTest() {
    List<String> path = JsAntlrPathBuilder.buildPath("x.y.partx", "x.y.partx".length() - 1).get();
    assertEquals(3, path.size());
    assertEquals("x", path.get(0));
    assertEquals("y", path.get(1));
    assertEquals("part", path.get(2));
  }

  @Test
  public void emptyPointTest() {
    List<String> path = JsAntlrPathBuilder.buildPath("x.y.", "x.y.".length()).get();
    assertEquals(3, path.size());
    assertEquals("x", path.get(0));
    assertEquals("y", path.get(1));
    assertEquals("", path.get(2));
  }

  @Test
  public void functionComplexArgumentsTest() {
    String code = "function xx() {" //
        + "callFunction();" //
        + "db . getCollection(x.name).count";

    List<String> path = JsAntlrPathBuilder.buildPath(code, code.length()).get();
    assertEquals(3, path.size());
    assertEquals("db", path.get(0));
    assertEquals("getCollection", path.get(1));
    assertEquals("count", path.get(2));
  }

  @Test
  public void functionCallTest() {
    String code = "function xx() {" //
        + "callFunction();" //
        + "db . getCollection(\"collection\").count";

    List<String> path = JsAntlrPathBuilder.buildPath(code, code.length()).get();
    assertEquals(3, path.size());
    assertEquals("db", path.get(0));
    assertEquals("getCollection", path.get(1));
    assertEquals("count", path.get(2));
  }

  @Test
  public void simplePathTest() {
    List<String> path = JsAntlrPathBuilder.buildPath("x.y.z", "x.y.z".length()).get();
    assertEquals(3, path.size());
    assertEquals("x", path.get(0));
    assertEquals("y", path.get(1));
    assertEquals("z", path.get(2));
  }
}
