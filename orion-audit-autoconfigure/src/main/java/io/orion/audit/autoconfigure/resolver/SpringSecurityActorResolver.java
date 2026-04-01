package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.core.model.ActorInfo;
import io.orion.audit.core.spi.ActorResolver;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves actor information from the Spring Security context.
 */
public class SpringSecurityActorResolver implements ActorResolver {

    private final List<AuditProperties.Actor> actorMappings;

    public SpringSecurityActorResolver() {
        this(List.of());
    }

    public SpringSecurityActorResolver(List<AuditProperties.Actor> actorMappings) {
        this.actorMappings = actorMappings == null ? List.of() : List.copyOf(new ArrayList<>(actorMappings));
    }

    @Override
    public ActorInfo resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ActorInfo.anonymous();
        }

        Object principal = authentication.getPrincipal();
        return resolveConfiguredActor(authentication, principal)
            .orElseGet(() -> defaultActor(authentication, principal));
    }

    private Optional<ActorInfo> resolveConfiguredActor(Authentication authentication, Object principal) {
        Optional<AuditProperties.Actor> fallback = Optional.empty();
        for (AuditProperties.Actor actor : actorMappings) {
            if (!hasText(actor.getPrincipalClass())) {
                fallback = Optional.of(actor);
                continue;
            }
            if (matchesPrincipal(actor.getPrincipalClass(), principal)) {
                return Optional.of(toActorInfo(authentication, principal, actor));
            }
        }
        return fallback.map(actor -> toActorInfo(authentication, principal, actor));
    }

    private boolean matchesPrincipal(String principalClassName, Object principal) {
        if (principal == null || !hasText(principalClassName)) {
            return false;
        }
        Class<?> current = principal.getClass();
        while (current != null && current != Object.class) {
            if (principalClassName.equals(current.getName())) {
                return true;
            }
            for (Class<?> contract : current.getInterfaces()) {
                if (principalClassName.equals(contract.getName())) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private ActorInfo toActorInfo(Authentication authentication, Object principal, AuditProperties.Actor actor) {
        BeanWrapperImpl beanWrapper = principal == null ? null : new BeanWrapperImpl(principal);
        String actorId = stringProperty(beanWrapper, actor.getIdProperty()).orElse(authentication.getName());
        String actorName = stringProperty(beanWrapper, actor.getNameProperty()).orElse(authentication.getName());
        String actorType = hasText(actor.getType()) ? actor.getType() : principalType(principal);
        String tenantId = stringProperty(beanWrapper, actor.getTenantIdProperty()).orElse(null);
        return new ActorInfo(actorId, actorName, actorType, tenantId);
    }

    private ActorInfo defaultActor(Authentication authentication, Object principal) {
        return new ActorInfo(authentication.getName(), authentication.getName(), principalType(principal), null);
    }

    private Optional<String> stringProperty(BeanWrapperImpl beanWrapper, String propertyName) {
        if (beanWrapper == null || !hasText(propertyName)) {
            return Optional.empty();
        }
        try {
            Object value = beanWrapper.getPropertyValue(propertyName);
            if (value == null) {
                return Optional.empty();
            }
            String text = String.valueOf(value);
            return hasText(text) ? Optional.of(text) : Optional.empty();
        }
        catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String principalType(Object principal) {
        return principal == null ? null : principal.getClass().getSimpleName();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
