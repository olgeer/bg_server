package com.ucsmy.itil.bg.api;

/**
 * Created by Max on 2017/2/22.
 */
public class LdapUser {
    private String account;
    private String accountWithDomain;
    private boolean disable;
    private String cn;
    private String ou;
    private String dc;

    public String getAccountWithDomain() {
        return accountWithDomain;
    }

    public void setAccountWithDomain(String accountWithDomain) {
        this.accountWithDomain = accountWithDomain;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getOu() {
        return ou;
    }

    public void setOu(String ou) {
        this.ou = ou;
    }

    public String getDc() {
        return dc;
    }

    public void setDc(String dc) {
        this.dc = dc;
    }
}
