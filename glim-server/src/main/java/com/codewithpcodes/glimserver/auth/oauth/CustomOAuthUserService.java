package com.codewithpcodes.glimserver.auth.oauth;

import com.codewithpcodes.glimserver.user.OAuthProvider;
import com.codewithpcodes.glimserver.user.Role;
import com.codewithpcodes.glimserver.user.User;
import com.codewithpcodes.glimserver.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomOAuthUserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        OAuth2UserInfo userInfo = new OAuth2UserInfo(oAuth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(
                    "Email not provided by Google. Check OAuth2 scope configuration"
            );
        }

        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
        User user = existingUser.map(
                value -> updateExistingUser(value, userInfo))
                .orElseGet(() -> registerNewUser(userInfo));

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserInfo userInfo) {
        String[] names = userInfo.getName().split(" ");
        String firstName = names[0];
        String lastName = Arrays.stream(names, 1, names.length)
                .collect(Collectors.joining(" "));

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(userInfo.getEmail())
                .oAuthProvider(OAuthProvider.GOOGLE)
                .providerId(userInfo.getId())
                .avatarKey(userInfo.getImageUrl())
                .role(Role.MEMBER)
                .build();
        return userRepository.save(user);
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        if (!user.getOAuthProvider().equals(OAuthProvider.GOOGLE)) {
            throw new OAuth2AuthenticationException("This email is already registered with a password. " +
                    "Please sign in  with your email and password");
        }
        user.setAvatarKey(userInfo.getImageUrl());
        user.setProviderId(userInfo.getId());
        return userRepository.save(user);
    }

}
