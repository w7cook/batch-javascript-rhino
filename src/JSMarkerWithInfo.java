public class JSMarkerWithInfo<T> {
  public final JSMarkers marker;
  public final T info;

  public JSMarkerWithInfo(JSMarkers marker, T info) {
    this.marker = marker;
    this.info = info;
  }
}
