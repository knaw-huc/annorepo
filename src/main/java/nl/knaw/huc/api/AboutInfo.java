package nl.knaw.huc.api;

public class AboutInfo {
  private String appName;
  private String startedAt;

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getAppName() {
    return appName;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

  public String getStartedAt() {
    return startedAt;
  }
}
