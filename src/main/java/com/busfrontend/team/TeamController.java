package com.busfrontend.team;

import com.busfrontend.members.MemberRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@Controller
@RequestMapping("/team")
public class TeamController {

    private final TeamRegistry registry;
    private final MemberRegistry memberRegistry;

    public TeamController(TeamRegistry registry, MemberRegistry memberRegistry) {
        this.registry = registry;
        this.memberRegistry = memberRegistry;
    }

    /** Team home page - 5 member cards with photo, name, modules, View Profile button. */
    @GetMapping
    public String teamHome(Model model) {
        model.addAttribute("members", registry.getAll());
        return "team/members";
    }

    /** Individual profile page. Slug must match one of the 5 defined members. */
    @GetMapping("/{slug}")
    public String profile(@PathVariable String slug, Model model, RedirectAttributes ra) {
        return registry.findBySlug(slug).map(m -> {
            model.addAttribute("member", m);
            // Cross-reference the Member Explorer registry so the profile page can
            // render the same "Services Owned" accordion as /members/{id} — matched
            // on name since both registries use the same human-readable names.
            memberRegistry.getAll().stream()
                    .filter(em -> em.getName().equalsIgnoreCase(m.getName()))
                    .findFirst()
                    .ifPresentOrElse(em -> {
                        model.addAttribute("explorerMember", em);
                        model.addAttribute("explorerServices", em.getServices());
                    }, () -> {
                        model.addAttribute("explorerServices", Collections.emptyList());
                    });
            return "team/profile";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Team member not found: " + slug);
            return "redirect:/team";
        });
    }
}
