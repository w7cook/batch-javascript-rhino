import java.util.Iterator;

public class Cons<T> implements Iterable<T> {
  public final T value;
  public final Cons<T> next;

  public Cons(T value, Cons<T> next) {
    this.value = value;
    this.next = next;
  }

  public Iterator<T> iterator() {
    final Cons<T> _curr = this;
    return new Iterator<T>() {
      private Cons<T> curr;
      { curr = _curr; }
      public boolean hasNext() { return !curr.isNil(); }
      public T next() {
        T v = curr.value;
        curr = curr.next;
        return v;
      }
      public void remove() { throw new UnsupportedOperationException(); }
    };
  }

  public boolean isNil() { return false; }

  public static class Nil<T> extends Cons<T> {
    public Nil() {
      super(null, null);
    }

    @Override
    public boolean isNil() { return true; }
  }
}
