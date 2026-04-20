package com.busfrontend.controller;

import com.busfrontend.client.AddressApiClient;
import com.busfrontend.client.BackendException;
import com.busfrontend.client.CustomerApiClient;
import com.busfrontend.dto.AddressDTO;
import com.busfrontend.dto.CustomerRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CustomerViewController {

    private final CustomerApiClient customerApiClient;
    private final AddressApiClient addressApiClient;

    @GetMapping("/view/customers")
    public String listCustomers(Model model) {
        model.addAttribute("customers", customerApiClient.getAll());
        return "customer/customers";
    }

    @GetMapping("/view/customers/add")
    public String showAddForm(Model model) {
        model.addAttribute("customer", new CustomerRequestDTO());
        return "customer/add-customer";
    }

    /**
     * Creates a new Address first from the inline form fields, then a Customer
     * pointing to that brand-new Address.
     */
    @PostMapping("/view/customers/save")
    public String saveCustomer(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam String address,
                               @RequestParam String city,
                               @RequestParam String state,
                               @RequestParam String zipCode,
                               RedirectAttributes ra) {
        try {
            AddressDTO addr = AddressDTO.builder()
                    .address(address)
                    .city(city)
                    .state(state)
                    .zipCode(zipCode)
                    .build();
            AddressDTO savedAddr = addressApiClient.create(addr);

            CustomerRequestDTO dto = new CustomerRequestDTO();
            dto.setName(name);
            dto.setEmail(email);
            dto.setPhone(phone);
            dto.setAddressId(savedAddr.getAddressId());
            customerApiClient.create(dto);

            ra.addFlashAttribute("message", "Customer added successfully.");
        } catch (BackendException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/view/customers";
    }

    @GetMapping("/view/customers/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("customer", customerApiClient.getById(id));
        model.addAttribute("addresses", addressApiClient.getAll());
        return "customer/update-customer";
    }

    @PostMapping("/view/customers/update/{id}")
    public String updateCustomer(@PathVariable Integer id,
                                 @ModelAttribute CustomerRequestDTO dto) {
        customerApiClient.update(id, dto);
        return "redirect:/view/customers";
    }
}
