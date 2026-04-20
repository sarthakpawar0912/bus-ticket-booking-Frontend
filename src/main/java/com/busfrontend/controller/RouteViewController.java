package com.busfrontend.controller;

import com.busfrontend.client.BackendException;
import com.busfrontend.client.RouteApiClient;
import com.busfrontend.dto.RouteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@SuppressWarnings("java:S4684")
@Controller
@RequiredArgsConstructor
public class RouteViewController {

    private static final String ATTR_MESSAGE = "message";
    private static final String ATTR_ERROR = "error";
    private static final String REDIRECT_VIEW_ROUTES = "redirect:/view/routes";

    private final RouteApiClient routeApiClient;

    @GetMapping("/view/routes")
    public String listRoutes(Model model) {
        model.addAttribute("routes", routeApiClient.getAll());
        return "route/routes";
    }

    @GetMapping("/view/routes/add")
    public String showAddForm(Model model) {
        model.addAttribute("route", new RouteDTO());
        return "route/add-route";
    }

    @PostMapping("/view/routes/save")
    public String saveRoute(@ModelAttribute RouteDTO route, RedirectAttributes ra) {
        try {
            routeApiClient.create(route);
            ra.addFlashAttribute(ATTR_MESSAGE, "Route added successfully!");
        } catch (BackendException ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        }
        return REDIRECT_VIEW_ROUTES;
    }

    @GetMapping("/view/routes/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("route", routeApiClient.getById(id));
        return "route/update-route";
    }

    @PostMapping("/view/routes/update/{id}")
    public String updateRoute(@PathVariable Integer id, @ModelAttribute RouteDTO route, RedirectAttributes ra) {
        try {
            routeApiClient.update(id, route);
            ra.addFlashAttribute(ATTR_MESSAGE, "Route updated successfully!");
        } catch (BackendException ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        }
        return REDIRECT_VIEW_ROUTES;
    }

    @GetMapping("/view/routes/search")
    public String searchRoutesView(@RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   Model model) {
        // TODO: advanced flow — RouteApiClient has no search endpoint yet; filter client-side.
        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            String fromTrim = from.trim().toLowerCase();
            String toTrim = to.trim().toLowerCase();
            var results = routeApiClient.getAll().stream()
                    .filter(r -> r.getFromCity() != null && r.getToCity() != null
                            && r.getFromCity().toLowerCase().contains(fromTrim)
                            && r.getToCity().toLowerCase().contains(toTrim))
                    .toList();
            model.addAttribute("results", results);
            model.addAttribute("from", from);
            model.addAttribute("to", to);
        }
        return "route/search-routes";
    }
}
