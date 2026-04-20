package com.busfrontend.controller;

import com.busfrontend.client.AgencyApiClient;
import com.busfrontend.dto.AgencyRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AgencyViewController {

    private final AgencyApiClient agencyApiClient;

    @GetMapping("/view/agencies")
    public String listAgencies(Model model) {
        model.addAttribute("agencies", agencyApiClient.getAll());
        return "agency/agencies";
    }

    @GetMapping("/view/agencies/add")
    public String showAddForm(Model model) {
        model.addAttribute("agency", new AgencyRequestDTO());
        return "agency/add-agency";
    }

    @PostMapping("/view/agencies/save")
    public String saveAgency(@ModelAttribute AgencyRequestDTO dto) {
        agencyApiClient.create(dto);
        return "redirect:/view/agencies";
    }

    @GetMapping("/view/agencies/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("agency", agencyApiClient.getById(id));
        return "agency/update-agency";
    }

    @PostMapping("/view/agencies/update/{id}")
    public String updateAgencyView(@PathVariable Integer id,
                                   @ModelAttribute AgencyRequestDTO dto) {
        agencyApiClient.update(id, dto);
        return "redirect:/view/agencies";
    }
}
