package com.busfrontend.controller;

import com.busfrontend.client.AddressApiClient;
import com.busfrontend.client.AgencyApiClient;
import com.busfrontend.client.AgencyOfficeApiClient;
import com.busfrontend.client.BackendException;
import com.busfrontend.dto.OfficeRequestDTO;
import com.busfrontend.dto.OfficeResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf view layer for Agency Offices (frontend mirror of the backend
 * OfficeViewController). Calls the backend REST API via AgencyOfficeApiClient.
 */
@Controller
@RequestMapping("/view/offices")
@RequiredArgsConstructor
public class OfficeViewController {

    private static final String REDIRECT = "redirect:/view/offices";

    private final AgencyOfficeApiClient officeApiClient;
    private final AgencyApiClient agencyApiClient;
    private final AddressApiClient addressApiClient;

    @GetMapping
    public String listOffices(Model model) {
        model.addAttribute("offices", officeApiClient.getAll());
        return "office/offices";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("office", new OfficeRequestDTO());
        model.addAttribute("agencies", agencyApiClient.getAll());
        model.addAttribute("addresses", addressApiClient.getAll());
        return "office/add-office";
    }

    @PostMapping("/save")
    public String saveOffice(@ModelAttribute("office") OfficeRequestDTO dto,
                             RedirectAttributes ra) {
        try {
            officeApiClient.create(dto);
            ra.addFlashAttribute("message", "Office added successfully!");
        } catch (BackendException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return REDIRECT;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        OfficeResponseDTO resp = officeApiClient.getById(id);
        // Map response -> request for form binding.
        OfficeRequestDTO form = new OfficeRequestDTO();
        if (resp != null) {
            form.setAgencyId(resp.getAgencyId());
            form.setOfficeMail(resp.getOfficeMail());
            form.setOfficeContactPersonName(resp.getOfficeContactPersonName());
            form.setOfficeContactNumber(resp.getOfficeContactNumber());
            form.setOfficeAddressId(resp.getOfficeAddressId());
        }
        model.addAttribute("office", form);
        model.addAttribute("officeId", id);
        model.addAttribute("agencies", agencyApiClient.getAll());
        model.addAttribute("addresses", addressApiClient.getAll());
        return "office/update-office";
    }

    @PostMapping("/update/{id}")
    public String updateOffice(@PathVariable Integer id,
                               @ModelAttribute("office") OfficeRequestDTO dto,
                               RedirectAttributes ra) {
        try {
            officeApiClient.update(id, dto);
            ra.addFlashAttribute("message", "Office updated successfully!");
        } catch (BackendException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return REDIRECT;
    }
}
