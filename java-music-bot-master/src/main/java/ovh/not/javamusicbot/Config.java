package ovh.not.javamusicbot;

import java.util.Set;

public class Config {
    public boolean dev = false;
    public boolean patreon = false;
    String token = null;
    public Set<String> owners = null;
    public Set<String> managers = null;
    public String regex = null;
    String prefix = null;
    String game = null;
    public String invite = null;
    public String about = null;
    public String join = null;
    public String carbon = null;
    public String dbots = null;
    public String dbotsOrg = null;
    public String discordServer = null;
    public String supporterRole = null;
    public String superSupporterRole = null;
    public String owoKey = null;
    public String glanceWebhook = null;
    public String statusWebhook = null;
    public String auditWebhook = null;
    public String statusToken = null;
    // 0 for main bot, 1 for patron 1, 2 for patron 2
    public Integer botIdentity = 0;
}
