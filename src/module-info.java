module jp.co.dgic.testing {
  requires transitive java.instrument;
  requires org.objectweb.asm;
  requires org.junit.jupiter.api;
  requires net.bytebuddy.agent;

  exports jp.co.dgic.testing.framework;
}