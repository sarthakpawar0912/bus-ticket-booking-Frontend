package com.busfrontend.controller;

import com.busfrontend.client.AddressApiClient;
import com.busfrontend.client.BackendException;
import com.busfrontend.client.BusApiClient;
import com.busfrontend.client.DriverApiClient;
import com.busfrontend.client.RouteApiClient;
import com.busfrontend.client.TripApiClient;
import com.busfrontend.dto.TripDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("java:S4684")
@Controller
@RequiredArgsConstructor
public class TripViewController {

    private static final String ATTR_MESSAGE = "message";
    private static final String ATTR_ERROR = "error";
    private static final String REDIRECT_VIEW_TRIPS = "redirect:/view/trips";

    private final TripApiClient tripApiClient;
    private final RouteApiClient routeApiClient;
    private final BusApiClient busApiClient;
    private final AddressApiClient addressApiClient;
    private final DriverApiClient driverApiClient;

    // ======================== THYMELEAF VIEWS ========================

    @GetMapping("/view/trips")
    public String listTrips(Model model) {
        List<TripDTO> trips = tripApiClient.getAllTrips();
        model.addAttribute("trips", trips);
        return "trip/trips";
    }

    @GetMapping("/view/trips/add")
    public String showAddForm(Model model) {
        model.addAttribute("routes", routeApiClient.getAll());
        model.addAttribute("buses", busApiClient.getAll());
        model.addAttribute("addresses", addressApiClient.getAll());
        model.addAttribute("drivers", driverApiClient.getAll());
        return "trip/add-trip";
    }

    @PostMapping("/view/trips/save")
    public String saveTrip(@RequestParam Integer routeId,
                           @RequestParam Integer busId,
                           @RequestParam Integer boardingAddressId,
                           @RequestParam Integer droppingAddressId,
                           @RequestParam String departureTime,
                           @RequestParam String arrivalTime,
                           @RequestParam Integer driver1Id,
                           @RequestParam(required = false) Integer driver2Id,
                           @RequestParam Integer availableSeats,
                           @RequestParam BigDecimal fare,
                           @RequestParam String tripDate,
                           RedirectAttributes ra) {
        try {
            TripDTO dto = TripDTO.builder()
                    .routeId(routeId)
                    .busId(busId)
                    .boardingAddressId(boardingAddressId)
                    .droppingAddressId(droppingAddressId)
                    .departureTime(LocalDateTime.parse(departureTime))
                    .arrivalTime(LocalDateTime.parse(arrivalTime))
                    .driver1Id(driver1Id)
                    .driver2Id(driver2Id)
                    .availableSeats(availableSeats)
                    .fare(fare)
                    .tripDate(LocalDateTime.parse(tripDate))
                    .build();
            tripApiClient.create(dto);
            ra.addFlashAttribute(ATTR_MESSAGE, "Trip added successfully!");
        } catch (BackendException ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        }
        return REDIRECT_VIEW_TRIPS;
    }

    @GetMapping("/view/trips/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        TripDTO trip = tripApiClient.getById(id);
        model.addAttribute("trip", trip);
        model.addAttribute("routes", routeApiClient.getAll());
        model.addAttribute("buses", busApiClient.getAll());
        model.addAttribute("addresses", addressApiClient.getAll());
        model.addAttribute("drivers", driverApiClient.getAll());
        return "trip/update-trip";
    }

    @PostMapping("/view/trips/update/{id}")
    public String updateTrip(@PathVariable Integer id,
                             @RequestParam Integer routeId,
                             @RequestParam Integer busId,
                             @RequestParam Integer boardingAddressId,
                             @RequestParam Integer droppingAddressId,
                             @RequestParam String departureTime,
                             @RequestParam String arrivalTime,
                             @RequestParam Integer driver1Id,
                             @RequestParam(required = false) Integer driver2Id,
                             @RequestParam Integer availableSeats,
                             @RequestParam BigDecimal fare,
                             @RequestParam String tripDate,
                             RedirectAttributes ra) {
        try {
            TripDTO dto = TripDTO.builder()
                    .tripId(id)
                    .routeId(routeId)
                    .busId(busId)
                    .boardingAddressId(boardingAddressId)
                    .droppingAddressId(droppingAddressId)
                    .departureTime(LocalDateTime.parse(departureTime))
                    .arrivalTime(LocalDateTime.parse(arrivalTime))
                    .driver1Id(driver1Id)
                    .driver2Id(driver2Id)
                    .availableSeats(availableSeats)
                    .fare(fare)
                    .tripDate(LocalDateTime.parse(tripDate))
                    .build();
            tripApiClient.update(id, dto);
            ra.addFlashAttribute(ATTR_MESSAGE, "Trip updated successfully!");
        } catch (BackendException ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        }
        return REDIRECT_VIEW_TRIPS;
    }

    @GetMapping("/view/trips/search")
    public String searchTripsView(@RequestParam(required = false) String from,
                                  @RequestParam(required = false) String to,
                                  Model model) {
        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            List<TripDTO> results = tripApiClient.search(from.trim(), to.trim());
            model.addAttribute("results", results);
            model.addAttribute("from", from);
            model.addAttribute("to", to);
        }
        return "trip/search-trips";
    }
}
