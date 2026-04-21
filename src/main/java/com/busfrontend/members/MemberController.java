package com.busfrontend.members;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/members")
public class MemberController {

    private static final String ATTR_MEMBER = "member";
    private static final String ATTR_ERROR = "error";
    private static final String PARAM_SERVICE = "serviceKey";
    private static final String PARAM_OPERATION = "operation";
    private static final String VIEW_OPERATION = "members/operation";

    private final MemberRegistry registry;
    private final OperationExecutor executor;
    private final RestTemplate restTemplate;

    @Value("${backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    public MemberController(MemberRegistry registry, OperationExecutor executor, RestTemplate restTemplate) {
        this.registry = registry;
        this.executor = executor;
        this.restTemplate = restTemplate;
    }

    // ---------- Listing ----------

    @GetMapping
    public String listMembers(Model model) {
        model.addAttribute("members", registry.getAll());
        return "members/members";
    }

    // ---------- Member detail (services + operations) ----------

    @GetMapping("/{id}")
    public String memberDetail(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return registry.findById(id).map(m -> {
            model.addAttribute(ATTR_MEMBER, m);
            return "members/member-detail";
        }).orElseGet(() -> {
            ra.addFlashAttribute(ATTR_ERROR, "Member not found with id " + id);
            return "redirect:/members";
        });
    }

    // ---------- Operation form ----------

    @GetMapping("/{id}/operation")
    public String showOperationForm(@PathVariable Integer id,
                                    @RequestParam String service,
                                    @RequestParam String operation,
                                    Model model,
                                    RedirectAttributes ra) {
        Member member = registry.findById(id).orElse(null);
        Operation op = registry.findOperation(id, service, operation).orElse(null);
        if (member == null || op == null) {
            ra.addFlashAttribute(ATTR_ERROR, "Operation not found.");
            return "redirect:/members/" + id;
        }
        model.addAttribute(ATTR_MEMBER, member);
        model.addAttribute(PARAM_SERVICE, service);
        model.addAttribute(PARAM_OPERATION, op);
        return VIEW_OPERATION;
    }

    // ---------- Execute operation ----------

    @PostMapping("/{id}/operation/execute")
    public String executeOperation(@PathVariable Integer id,
                                   @RequestParam String service,
                                   @RequestParam String operation,
                                   @RequestParam(required = false) String pathId,
                                   @RequestParam Map<String, String> allParams,
                                   Model model,
                                   RedirectAttributes ra) {
        Member member = registry.findById(id).orElse(null);
        Operation op = registry.findOperation(id, service, operation).orElse(null);
        if (member == null || op == null) {
            ra.addFlashAttribute(ATTR_ERROR, "Operation not found.");
            return "redirect:/members/" + id;
        }

        String kind = op.getInputKind();
        if ("PDF_DOWNLOAD".equals(kind)) {
            return handlePdfDownload(op, pathId, member, service, model);
        }
        if ("PDF_DOWNLOAD_QUERY".equals(kind)) {
            return handlePdfDownloadQuery(op, allParams);
        }

        Map<String, String> formData = stripRoutingParams(allParams);
        OperationExecutor.ExecutionResult result = executor.execute(op, pathId, formData);

        model.addAttribute(ATTR_MEMBER, member);
        model.addAttribute(PARAM_SERVICE, service);
        model.addAttribute(PARAM_OPERATION, op);
        model.addAttribute("submittedPathId", pathId);
        model.addAttribute("submittedForm", formData);
        model.addAttribute("result", result);
        return VIEW_OPERATION;
    }

    private String handlePdfDownload(Operation op, String pathId, Member member,
                                     String service, Model model) {
        if (pathId == null || pathId.isBlank()) {
            model.addAttribute(ATTR_ERROR, "ID is required to download the PDF.");
            model.addAttribute(ATTR_MEMBER, member);
            model.addAttribute(PARAM_SERVICE, service);
            model.addAttribute(PARAM_OPERATION, op);
            return VIEW_OPERATION;
        }
        return "redirect:" + backendBaseUrl + op.getEndpoint().replace("{id}", pathId.trim());
    }

    private String handlePdfDownloadQuery(Operation op, Map<String, String> allParams) {
        Map<String, String> q = stripRoutingParams(allParams);
        StringBuilder qs = new StringBuilder();
        for (Map.Entry<String, String> e : q.entrySet()) {
            String v = e.getValue();
            if (v == null || v.isBlank()) continue;
            if (!qs.isEmpty()) qs.append('&');
            qs.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        }
        return "redirect:" + backendBaseUrl + op.getEndpoint() + (!qs.isEmpty() ? "?" + qs : "");
    }

    private Map<String, String> stripRoutingParams(Map<String, String> allParams) {
        Map<String, String> m = new HashMap<>(allParams);
        m.remove("service");
        m.remove(PARAM_OPERATION);
        m.remove("pathId");
        return m;
    }

    // ---------- Quick PDF download proxy (for the direct Download button) ----------

    @GetMapping("/{id}/operation/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Integer id,
                                              @RequestParam String service,
                                              @RequestParam String operation,
                                              @RequestParam String pathId,
                                              HttpServletRequest req) {
        Operation op = registry.findOperation(id, service, operation).orElse(null);
        if (op == null || !"PDF_DOWNLOAD".equals(op.getInputKind())) {
            return ResponseEntity.notFound().build();
        }
        String url = backendBaseUrl + op.getEndpoint().replace("{id}", pathId);
        byte[] pdf = restTemplate.getForObject(url, byte[].class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ticket-" + pathId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
