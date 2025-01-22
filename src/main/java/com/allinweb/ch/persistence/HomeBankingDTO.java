package com.allinweb.ch.persistence;

import java.io.Serializable;
import java.util.List;
import javax.persistence.*;

@Entity()
@Table(name = "home_banking")
// @SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "homeBankingSeq", allocationSize = 1)
public class HomeBankingDTO extends BaseDTO implements Serializable {
    @Column(name = "url")
    private String url;

    @Column(name = "name")
    @OrderBy("name DESC")
    private String name;

    @Column(name = "priority")
    private String priority;

    @Column(name = "search_config")
    private String searchConfig;

    @Column(name = "options_config")
    private String optionsConfig;

    @Column(name = "cookies")
    private String cookies;

    @Column(name = "driver_session")
    private String driverSession;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("name DESC")
    @JoinColumn(name = "home_banking_id")
    private List<BotJobDTO> botJobDTOS;

    public HomeBankingDTO() {
        super();
    }

    public HomeBankingDTO(int id) {
        super(id);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSearchConfig() {
        return searchConfig;
    }

    public void setSearchConfig(String searchConfig) {
        this.searchConfig = searchConfig;
    }

    public String getOptionsConfig() {
        return optionsConfig;
    }

    public void setOptionsConfig(String optionsConfig) {
        this.optionsConfig = optionsConfig;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public String getDriverSession() {
        return driverSession;
    }

    public void setDriverSession(String driverSession) {
        this.driverSession = driverSession;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<BotJobDTO> getBotJobs() {
        return botJobDTOS;
    }

    public void setBotJobs(List<BotJobDTO> botJobDTOS) {
        this.botJobDTOS = botJobDTOS;
    }
}
