public interface MonadI<Self, A> {
  public <B> MonadI<Self, B> Bind(Function<A, ? extends MonadI<Self, B>> f);
}
