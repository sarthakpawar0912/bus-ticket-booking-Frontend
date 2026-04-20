package com.busfrontend.controller;

import com.busfrontend.client.AddressApiClient;
import com.busfrontend.client.AgencyOfficeApiClient;
import com.busfrontend.client.DriverApiClient;
import com.busfrontend.dto.DriverRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class DriverViewController {

    private static final String REDIRECT_VIEW_DRIVERS = "redirect:/view/drivers";

    private final DriverApiClient driverApiClient;
    private final AgencyOfficeApiClient agencyOfficeApiClient;
    private final AddressApiClient addressApiClient;

    @GetMapping("/view/drivers")
    public String listDrivers(Model model) {
        model.addAttribute("drivers", driverApiClient.getAll());
        return "driver/drivers";
    }

    @GetMapping("/view/drivers/add")
    public String showAddForm(Model model) {
        model.addAttribute("driver", new DriverRequestDTO());
        model.addAttribute("offices", agencyOfficeApiClient.getAll());
        model.addAttribute("addresses", addressApiClient.getAll());
        return "driver/add-driver";
    }

    @PostMapping("/view/drivers/save")
    public String saveDriver(@ModelAttribute DriverRequestDTO dto) {
        driverApiClient.create(dto);
        return REDIRECT_VIEW_DRIVERS;
    }

    @GetMapping("/view/drivers/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("driver", driverApiClient.getById(id));
        model.addAttribute("offices", agencyOfficeApiClient.getAll());
        model.addAttribute("addresses", addressApiClient.getAll());
        return "driver/update-driver";
    }

    @PostMapping("/view/drivers/update/{id}")
    public String updateDriverView(@PathVariable Integer id,
                                   @ModelAttribute DriverRequestDTO dto) {
        driverApiClient.update(id, dto);
        return REDIRECT_VIEW_DRIVERS;
    }
}
