package com.allinweb.ch.dto;

import java.util.List;
import javax.persistence.*;

@Entity()
@Table(name = "home_banking")
@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "homeBankingSeq", allocationSize = 1)
public class HomeBankingDTO extends BaseDTO {
    @Column(name = "url")
    private String url;

    @Column(name = "name")
    @OrderBy("name DESC")
    private String name;

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
