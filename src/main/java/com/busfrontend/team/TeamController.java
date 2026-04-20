package com.busfrontend.team;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/team")
public class TeamController {

    private final TeamRegistry registry;

    public TeamController(TeamRegistry registry) {
        this.registry = registry;
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
            return "team/profile";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Team member not found: " + slug);
            return "redirect:/team";
        });
    }
}
