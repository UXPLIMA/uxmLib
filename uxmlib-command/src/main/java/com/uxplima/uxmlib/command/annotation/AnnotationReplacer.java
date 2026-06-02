package com.uxplima.uxmlib.command.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

/**
 * A registration-time rewrite of one custom annotation into core annotation(s), the seam by which a consumer
 * teaches the command DSL a shorthand of its own. When an element (a {@code @}{@link
 * com.uxplima.uxmlib.command.annotation.annotations.Command} class, a {@code @}{@link
 * com.uxplima.uxmlib.command.annotation.annotations.Subcommand} method, or an {@code @}{@link
 * com.uxplima.uxmlib.command.annotation.annotations.Arg} parameter) carries an annotation of the registered
 * type {@code A}, the replacer is asked for the core annotations it stands for; the model is then built from
 * those <em>effective</em> annotations rather than the raw ones, so every read past registration honours the
 * rewrite. This mirrors Lamp's {@code AnnotationReplacer}.
 *
 * <p>Register one with {@link ParamResolvers#replacer(Class, AnnotationReplacer)}. A replacer that has nothing
 * to contribute for a given element returns an empty list (never {@code null}); the source annotation it
 * matched is dropped from the effective view and the returned ones take its place.
 *
 * <pre>{@code
 * // @Admin("shop.admin") on a branch becomes @Permission("shop.admin")
 * ParamResolvers.withDefaults()
 *     .replacer(Admin.class, (admin, element) -> List.of(Replacements.permission(admin.value())));
 * }</pre>
 *
 * @param <A> the custom annotation type this replacer consumes
 */
@FunctionalInterface
public interface AnnotationReplacer<A extends Annotation> {

    /**
     * The core annotations the custom {@code annotation} on {@code element} stands for. Returns an empty list
     * to contribute nothing (the source annotation is simply removed from the effective view). Must not return
     * {@code null}; a {@code null} entry inside the list is rejected when the effective view is assembled.
     *
     * @param annotation the matched custom annotation read off {@code element}
     * @param element the class, method, or parameter the annotation sits on
     * @return the replacement annotations, in the order they should apply
     */
    List<Annotation> replace(A annotation, AnnotatedElement element);
}
