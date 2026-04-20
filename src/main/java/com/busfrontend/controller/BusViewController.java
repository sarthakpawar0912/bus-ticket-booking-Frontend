package com.busfrontend.controller;

import com.busfrontend.client.AgencyOfficeApiClient;
import com.busfrontend.client.BusApiClient;
import com.busfrontend.dto.BusRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class BusViewController {

    private static final String REDIRECT_VIEW_BUSES = "redirect:/view/buses";

    private final BusApiClient busApiClient;
    private final AgencyOfficeApiClient agencyOfficeApiClient;

    @GetMapping("/view/buses")
    public String listBuses(Model model) {
        model.addAttribute("buses", busApiClient.getAll());
        return "bus/buses";
    }

    @GetMapping("/view/buses/add")
    public String showAddForm(Model model) {
        model.addAttribute("bus", new BusRequestDTO());
        model.addAttribute("offices", agencyOfficeApiClient.getAll());
        return "bus/add-bus";
    }

    @PostMapping("/view/buses/save")
    public String saveBus(@ModelAttribute BusRequestDTO dto) {
        busApiClient.create(dto);
        return REDIRECT_VIEW_BUSES;
    }

    @GetMapping("/view/buses/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("bus", busApiClient.getById(id));
        model.addAttribute("offices", agencyOfficeApiClient.getAll());
        return "bus/update-bus";
    }

    @PostMapping("/view/buses/update/{id}")
    public String updateBusView(@PathVariable Integer id,
                                @ModelAttribute BusRequestDTO dto) {
        busApiClient.update(id, dto);
        return REDIRECT_VIEW_BUSES;
    }
}
