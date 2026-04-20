package com.busfrontend.controller;

import com.busfrontend.client.BackendException;
import com.busfrontend.client.CustomerApiClient;
import com.busfrontend.client.ReviewApiClient;
import com.busfrontend.client.TripApiClient;
import com.busfrontend.dto.ReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReviewViewController {

    private final ReviewApiClient reviewApiClient;
    private final CustomerApiClient customerApiClient;
    private final TripApiClient tripApiClient;

    @GetMapping("/view/reviews")
    public String listReviews(Model model) {
        List<Map<String, Object>> reviews = reviewApiClient.getAll();
        model.addAttribute("reviews", reviews);
        return "review/reviews";
    }

    @GetMapping("/view/reviews/add")
    public String showAddReviewForm(Model model) {
        if (!model.containsAttribute("review")) {
            model.addAttribute("review", new ReviewDTO());
        }
        model.addAttribute("customers", customerApiClient.getAll());
        model.addAttribute("trips", tripApiClient.getAllTrips());
        return "review/add-review";
    }

    @PostMapping("/view/reviews/save")
    public String saveReview(@ModelAttribute("review") ReviewDTO reviewDTO,
                             RedirectAttributes redirectAttributes) {
        try {
            reviewApiClient.create(reviewDTO);
            redirectAttributes.addFlashAttribute("success", "Review created successfully");
            return "redirect:/view/reviews";
        } catch (BackendException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("review", reviewDTO);
            return "redirect:/view/reviews/add";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("review", reviewDTO);
            return "redirect:/view/reviews/add";
        }
    }
}
