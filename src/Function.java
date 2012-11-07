abstract public class Function<I,O> {
  abstract O call(I in);

  public static <A,B> Function<A,B> Const(final B _b) {
    return new Function<A,B>() {
      public B call(A a) { return _b; }
    };
  }
}
