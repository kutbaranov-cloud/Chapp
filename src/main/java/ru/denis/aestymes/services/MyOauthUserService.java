package ru.denis.aestymes.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.repositories.MyUserRepository;

import java.util.Map;

@Service
public class MyOauthUserService extends DefaultOAuth2UserService {

    @Autowired
    private MyUserRepository myUserRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        if (email != null && myUserRepository.findMyUserByEmail(email) == null) {
            MyUser user = new MyUser();
            user.setEmail(email);
            user.setName((String) attributes.getOrDefault("name", email));
            user.setUsername(email.split("@")[0] + "_" + System.currentTimeMillis() % 1000);
            user.setVerified(true);
            myUserRepository.save(user);
        }
        return oAuth2User;
    }
}