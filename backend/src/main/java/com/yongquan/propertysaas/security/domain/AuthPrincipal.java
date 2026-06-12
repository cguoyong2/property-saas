package com.yongquan.propertysaas.security.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class AuthPrincipal implements Authentication {

    private final CurrentUser currentUser;
    private final List<? extends GrantedAuthority> authorities;
    private boolean authenticated = true;

    public AuthPrincipal(CurrentUser currentUser, List<? extends GrantedAuthority> authorities) {
        this.currentUser = currentUser;
        this.authorities = authorities;
    }

    public CurrentUser currentUser() {
        return currentUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getDetails() {
        return currentUser;
    }

    @Override
    public Object getPrincipal() {
        return currentUser;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        this.authenticated = authenticated;
    }

    @Override
    public String getName() {
        return currentUser.username();
    }
}
