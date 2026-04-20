package com.busfrontend.controller;

import com.busfrontend.client.AddressApiClient;
import com.busfrontend.dto.AddressDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SuppressWarnings("java:S4684")
@Controller
@RequiredArgsConstructor
public class AddressViewController {

    private static final String REDIRECT_VIEW_ADDRESSES = "redirect:/view/addresses";

    private final AddressApiClient addressApiClient;

    @GetMapping("/view/addresses")
    public String listAddresses(Model model) {
        model.addAttribute("addresses", addressApiClient.getAll());
        return "address/addresses";
    }

    @GetMapping("/view/addresses/add")
    public String showAddForm(Model model) {
        model.addAttribute("address", new AddressDTO());
        return "address/add-address";
    }

    @PostMapping("/view/addresses/save")
    public String saveAddress(@RequestParam String address,
                              @RequestParam String city,
                              @RequestParam String state,
                              @RequestParam String zipCode) {
        AddressDTO addr = AddressDTO.builder()
                .address(address)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .build();
        addressApiClient.create(addr);
        return REDIRECT_VIEW_ADDRESSES;
    }

    @GetMapping("/view/addresses/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("address", addressApiClient.getById(id));
        return "address/update-address";
    }

    @PostMapping("/view/addresses/update/{id}")
    public String updateAddress(@PathVariable Integer id,
                                @RequestParam String address,
                                @RequestParam String city,
                                @RequestParam String state,
                                @RequestParam String zipCode) {
        AddressDTO addr = AddressDTO.builder()
                .addressId(id)
                .address(address)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .build();
        addressApiClient.update(id, addr);
        return REDIRECT_VIEW_ADDRESSES;
    }
}
