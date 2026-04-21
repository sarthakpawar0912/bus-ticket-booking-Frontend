package com.busfrontend.members;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Member -> Service -> Operation allocation. Mirrors the team page
 * (see {@link com.busticketbookingsystem.web.team.TeamRegistry}) so the
 * two views stay in sync.
 */
@Component
public class MemberRegistry {

    private final List<Member> members = buildMembers();

    public List<Member> getAll() {
        return members;
    }

    public Optional<Member> findById(Integer id) {
        return members.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    public Optional<Operation> findOperation(Integer memberId, String serviceKey, String operationName) {
        return findById(memberId).flatMap(m -> m.getServices().stream()
                .filter(s -> s.getKey().equalsIgnoreCase(serviceKey))
                .findFirst()
                .flatMap(s -> s.getOperations().stream()
                        .filter(o -> o.getName().equalsIgnoreCase(operationName))
                        .findFirst()));
    }

    // ---------- Shared literal constants (SonarQube S1192) ----------

    private static final String T_STRING = "string";
    private static final String T_INTEGER = "integer";
    private static final String T_NUMBER = "number";
    private static final String T_EMAIL_IN = "email";
    private static final String T_DATETIME = "datetime-local";
    private static final String F_EMAIL = "email";
    private static final String F_PHONE = "phone";
    private static final String F_PHONE_LABEL = "Phone (10 digits)";
    private static final String F_CUSTOMER_ID = "customerId";
    private static final String F_CUSTOMER_ID_LABEL = "Customer ID";
    private static final String CITY_MUMBAI = "Mumbai";
    private static final String OP_GET_ALL = "Get All";
    private static final String OP_GET_BY_ID = "Get By ID";
    private static final String OP_CREATE = "Create";
    private static final String OP_UPDATE = "Update";

    // ---------- Field builders ----------

    private static FieldDef f(String name, String label, String type, String placeholder, boolean required, String valueType) {
        return FieldDef.builder().name(name).label(label).type(type)
                .placeholder(placeholder).required(required).valueType(valueType).build();
    }

    // ---------- DTO field definitions ----------

    private static final List<FieldDef> CUSTOMER_FIELDS = List.of(
            f("name", "Full Name", "text", "Sarthak Pawar", true, T_STRING),
            f(F_EMAIL, "Email", T_EMAIL_IN, "sarthak@example.com", true, T_STRING),
            f(F_PHONE, F_PHONE_LABEL, "tel", "9876543210", true, T_STRING),
            f("addressId", "Address ID", T_NUMBER, "1", true, T_INTEGER));

    private static final List<FieldDef> ADDRESS_FIELDS = List.of(
            f("address", "Street Address", "text", "Station Road", true, T_STRING),
            f("city", "City", "text", CITY_MUMBAI, true, T_STRING),
            f("state", "State", "text", "Maharashtra", true, T_STRING),
            f("zipCode", "Zip Code (6 digits)", "text", "400001", true, T_STRING));

    private static final List<FieldDef> AGENCY_FIELDS = List.of(
            f("name", "Agency Name", "text", "Red Bus Agency", true, T_STRING),
            f("contactPersonName", "Contact Person Name", "text", "Sarthak", true, T_STRING),
            f(F_EMAIL, "Email", T_EMAIL_IN, "red@bus.com", true, T_STRING),
            f(F_PHONE, F_PHONE_LABEL, "tel", "9876543210", true, T_STRING));

    private static final List<FieldDef> OFFICE_FIELDS = List.of(
            f("agencyId", "Agency ID", T_NUMBER, "1", true, T_INTEGER),
            f("officeMail", "Office Email", T_EMAIL_IN, "mumbai@red.com", true, T_STRING),
            f("officeContactPersonName", "Office Contact Person", "text", "Amit", true, T_STRING),
            f("officeContactNumber", "Office Phone (10 digits)", "tel", "9999999999", true, T_STRING),
            f("officeAddressId", "Office Address ID", T_NUMBER, "1", true, T_INTEGER));

    private static final List<FieldDef> BUS_FIELDS = List.of(
            f("officeId", "Office ID", T_NUMBER, "1", true, T_INTEGER),
            f("registrationNumber", "Registration Number", "text", "MH01AB1234", true, T_STRING),
            f("capacity", "Capacity (min 10)", T_NUMBER, "40", true, T_INTEGER),
            f("type", "Bus Type", "text", "AC Sleeper", true, T_STRING));

    private static final List<FieldDef> DRIVER_FIELDS = List.of(
            f("licenseNumber", "License Number", "text", "DL001", true, T_STRING),
            f("name", "Driver Name", "text", "Ramesh", true, T_STRING),
            f(F_PHONE, F_PHONE_LABEL, "tel", "7777777777", true, T_STRING),
            f("officeId", "Office ID", T_NUMBER, "1", true, T_INTEGER),
            f("addressId", "Address ID", T_NUMBER, "1", true, T_INTEGER));

    private static final List<FieldDef> ROUTE_FIELDS = List.of(
            f("fromCity", "From City", "text", CITY_MUMBAI, true, T_STRING),
            f("toCity", "To City", "text", "Pune", true, T_STRING),
            f("breakPoints", "Break Points", T_NUMBER, "1", false, T_INTEGER),
            f("duration", "Duration (minutes)", T_NUMBER, "180", false, T_INTEGER));

    private static final List<FieldDef> ROUTE_SEARCH_FIELDS = List.of(
            f("from", "From City", "text", CITY_MUMBAI, true, T_STRING),
            f("to", "To City", "text", "Pune", true, T_STRING));

    private static final List<FieldDef> TRIP_FIELDS = List.of(
            f("routeId", "Route ID", T_NUMBER, "1", true, T_INTEGER),
            f("busId", "Bus ID", T_NUMBER, "1", true, T_INTEGER),
            f("boardingAddressId", "Boarding Address ID", T_NUMBER, "1", true, T_INTEGER),
            f("droppingAddressId", "Dropping Address ID", T_NUMBER, "2", true, T_INTEGER),
            f("departureTime", "Departure Time", T_DATETIME, "", true, T_STRING),
            f("arrivalTime", "Arrival Time", T_DATETIME, "", true, T_STRING),
            f("driver1Id", "Driver 1 ID", T_NUMBER, "1", true, T_INTEGER),
            f("driver2Id", "Driver 2 ID (optional)", T_NUMBER, "2", false, T_INTEGER),
            f("availableSeats", "Available Seats", T_NUMBER, "40", true, T_INTEGER),
            f("fare", "Fare (Rs.)", T_NUMBER, "500", true, T_NUMBER),
            f("tripDate", "Trip Date", T_DATETIME, "", true, T_STRING));

    private static final List<FieldDef> TRIP_SEARCH_FIELDS = ROUTE_SEARCH_FIELDS;

    private static final List<FieldDef> PAYMENT_FIELDS = List.of(
            f("bookingId", "Booking ID", T_NUMBER, "1", true, T_INTEGER),
            f(F_CUSTOMER_ID, F_CUSTOMER_ID_LABEL, T_NUMBER, "10", true, T_INTEGER),
            f("amount", "Amount (Rs.)", T_NUMBER, "500", true, T_NUMBER));

    private static final List<FieldDef> REVIEW_FIELDS = List.of(
            f(F_CUSTOMER_ID, F_CUSTOMER_ID_LABEL, T_NUMBER, "1", true, T_INTEGER),
            f("tripId", "Trip ID", T_NUMBER, "1", true, T_INTEGER),
            f("rating", "Rating (1-5)", T_NUMBER, "5", true, T_INTEGER),
            f("comment", "Comment", "textarea", "Great experience!", false, T_STRING));

    private static final List<FieldDef> BOOKING_FIELDS = List.of(
            f("tripId", "Trip ID", T_NUMBER, "1", true, T_INTEGER),
            f("seatNumbers", "Seat Numbers (comma-separated)", "text", "1,2,3", true, "integer-list"),
            f(F_CUSTOMER_ID, F_CUSTOMER_ID_LABEL, T_NUMBER, "10", true, T_INTEGER));

    private static final List<FieldDef> GROUP_TICKET_FIELDS = List.of(
            f("bookingIds", "Booking IDs (comma-separated)", "text", "1,2,3", true, T_STRING));

    // ---------- CRUD builders (matches team page: Get All, Create, Update) ----------

    private static Operation op(String name, String method, String endpoint, String inputKind, String desc, List<FieldDef> fields) {
        return Operation.builder().name(name).method(method).endpoint(endpoint)
                .inputKind(inputKind).description(desc).fields(fields).build();
    }

    private static List<Operation> crudOps(String base, List<FieldDef> fields) {
        return List.of(
                op(OP_GET_ALL, "GET", base, "NONE", "Fetch all records", List.of()),
                op(OP_GET_BY_ID, "GET", base + "/{id}", "ID", "Fetch a single record by ID", List.of()),
                op(OP_CREATE, "POST", base, "BODY", "Create a new record", fields),
                op(OP_UPDATE, "PUT", base + "/{id}", "ID_AND_BODY", "Update an existing record", fields)
        );
    }

    // ---------- Service builders ----------

    private static ServiceInfo customerService() {
        return ServiceInfo.builder().key("customers").name("Customers").icon("bi-people-fill")
                .description("Manage customer profiles and contact details")
                .operations(crudOps("/api/customers", CUSTOMER_FIELDS)).build();
    }

    private static ServiceInfo addressService() {
        return ServiceInfo.builder().key("addresses").name("Addresses").icon("bi-geo-alt-fill")
                .description("Manage addresses used by customers, offices, and trips")
                .operations(crudOps("/api/addresses", ADDRESS_FIELDS)).build();
    }

    private static ServiceInfo agencyService() {
        return ServiceInfo.builder().key("agencies").name("Agencies").icon("bi-building")
                .description("Manage bus agencies and their offices")
                .operations(crudOps("/api/agencies", AGENCY_FIELDS)).build();
    }

    private static ServiceInfo officeService() {
        return ServiceInfo.builder().key("offices").name("Agency Offices").icon("bi-shop")
                .description("Manage office branches linked to agencies")
                .operations(crudOps("/api/offices", OFFICE_FIELDS)).build();
    }

    private static ServiceInfo busService() {
        return ServiceInfo.builder().key("buses").name("Buses").icon("bi-bus-front-fill")
                .description("Manage physical buses with capacity and registration")
                .operations(crudOps("/api/buses", BUS_FIELDS)).build();
    }

    private static ServiceInfo driverService() {
        return ServiceInfo.builder().key("drivers").name("Drivers").icon("bi-person-badge")
                .description("Manage licensed drivers")
                .operations(crudOps("/api/drivers", DRIVER_FIELDS)).build();
    }

    private static ServiceInfo routeService() {
        List<Operation> ops = new ArrayList<>(crudOps("/api/routes", ROUTE_FIELDS));
        ops.add(op("Search", "GET", "/api/routes/search", "QUERY",
                "Search routes by from/to city", ROUTE_SEARCH_FIELDS));
        return ServiceInfo.builder().key("routes").name("Routes").icon("bi-signpost-split")
                .description("Manage from-city -> to-city routes")
                .operations(ops).build();
    }

    private static ServiceInfo tripService() {
        List<Operation> ops = new ArrayList<>(crudOps("/api/trips", TRIP_FIELDS));
        ops.add(op("Search", "GET", "/api/trips/search", "QUERY",
                "Search trips by from/to city", TRIP_SEARCH_FIELDS));
        return ServiceInfo.builder().key("trips").name("Trips").icon("bi-calendar-event")
                .description("Manage scheduled trips with buses, drivers, fare")
                .operations(ops).build();
    }

    // ---- Member 4: Anushka Bankar — Payment Module ----

    private static ServiceInfo paymentService() {
        List<Operation> ops = List.of(
                op(OP_GET_ALL, "GET", "/api/payments", "NONE", "Fetch all payments", List.of()),
                op(OP_GET_BY_ID, "GET", "/api/payments/{id}", "ID", "Fetch a single payment by ID", List.of()),
                op(OP_CREATE, "POST", "/api/payments", "BODY", "Process a new payment", PAYMENT_FIELDS)
        );
        return ServiceInfo.builder().key("payments").name("Payments").icon("bi-credit-card")
                .description("Process payments and lookups")
                .operations(ops).build();
    }

    private static ServiceInfo paymentTicketService() {
        List<Operation> ops = List.of(
                op("Download Payment Ticket", "GET", "/api/payments/{id}/ticket", "PDF_DOWNLOAD",
                        "Download a payment ticket PDF (auto-generates group ticket when the payment is part of a multi-seat transaction)",
                        List.of())
        );
        return ServiceInfo.builder().key("pdf-payment").name("Ticket Download").icon("bi-file-earmark-pdf-fill")
                .description("Download payment ticket PDFs")
                .operations(ops).build();
    }

    // ---- Member 5: Kedar Mahadik — Booking + Reviews ----

    private static ServiceInfo bookingService() {
        List<Operation> ops = List.of(
                op(OP_GET_BY_ID, "GET", "/api/bookings/{id}", "ID",
                        "Fetch a single booking by ID", List.of()),
                op("Get By Trip", "GET", "/api/bookings/trip/{id}", "ID",
                        "List seats/bookings for a trip", List.of()),
                op(OP_CREATE, "POST", "/api/bookings", "BODY", "Book one or more seats", BOOKING_FIELDS)
        );
        return ServiceInfo.builder().key("bookings").name("Bookings").icon("bi-ticket-perforated")
                .description("Reserve seats and view booking status")
                .operations(ops).build();
    }

    private static ServiceInfo bookingTicketService() {
        List<Operation> ops = List.of(
                op("Download Booking Ticket", "GET", "/api/bookings/{id}/ticket", "PDF_DOWNLOAD",
                        "Generate a boarding-pass PDF for a booking", List.of()),
                op("Download Group Ticket", "GET", "/api/bookings/group-ticket", "PDF_DOWNLOAD_QUERY",
                        "Download group booking ticket PDF (multiple bookings)", GROUP_TICKET_FIELDS)
        );
        return ServiceInfo.builder().key("pdf-booking").name("Ticket Download").icon("bi-file-earmark-pdf-fill")
                .description("Generate downloadable booking PDFs (single + group)")
                .operations(ops).build();
    }

    private static ServiceInfo reviewService() {
        List<Operation> ops = List.of(
                op(OP_GET_ALL, "GET", "/api/reviews", "NONE", "Fetch all reviews", List.of()),
                op(OP_GET_BY_ID, "GET", "/api/reviews/{id}", "ID", "Fetch a single review by ID", List.of()),
                op(OP_CREATE, "POST", "/api/reviews", "BODY", "Post a new review", REVIEW_FIELDS)
        );
        return ServiceInfo.builder().key("reviews").name("Reviews").icon("bi-star-fill")
                .description("Customer feedback and ratings for trips")
                .operations(ops).build();
    }

    // ---------- Team ----------

    private List<Member> buildMembers() {
        return List.of(
                Member.builder().id(1).name("Sarthak Pawar").role("Customer & Address Module")
                        .initials("SP").color("#0d6efd")
                        .image("/images/Sarthak.png")
                        .summary("Customers, Addresses")
                        .services(List.of(customerService(), addressService())).build(),

                Member.builder().id(2).name("Atharv Kadam").role("Agency, Bus & Driver Module")
                        .initials("AK").color("#198754")
                        .image("/images/Atharv.png")
                        .summary("Agencies, Offices, Buses, Drivers")
                        .services(List.of(agencyService(), officeService(), busService(), driverService())).build(),

                Member.builder().id(3).name("Atharva Pawar").role("Route & Trip Module")
                        .initials("AP").color("#fd7e14")
                        .image("/images/Atharva.jpeg")
                        .summary("Routes, Trips, Search")
                        .services(List.of(routeService(), tripService())).build(),

                Member.builder().id(4).name("Anushka Bankar").role("Payment Module")
                        .initials("AB").color("#6f42c1")
                        .image("/images/Anushka.png")
                        .summary("Payments, Ticket Download")
                        .services(List.of(paymentService(), paymentTicketService())).build(),

                Member.builder().id(5).name("Kedar Mahadik").role("Booking & Reviews Module")
                        .initials("KM").color("#dc3545")
                        .image("/images/Kedar.jpg")
                        .summary("Bookings, Ticket Download, Group Ticket, Reviews")
                        .services(List.of(bookingService(), bookingTicketService(), reviewService())).build()
        );
    }
}
