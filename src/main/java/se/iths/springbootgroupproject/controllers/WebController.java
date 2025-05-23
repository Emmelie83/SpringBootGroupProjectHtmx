package se.iths.springbootgroupproject.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import se.iths.springbootgroupproject.dtos.CreateMessageFormData;
import se.iths.springbootgroupproject.entities.Message;
import se.iths.springbootgroupproject.entities.User;
import se.iths.springbootgroupproject.services.MessageService;
import se.iths.springbootgroupproject.services.TranslationService;
import se.iths.springbootgroupproject.services.UserService;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/web")
public class WebController {
    MessageService messageService;
    UserService userService;
    TranslationService translationService;

    public WebController(MessageService messageService, UserService userService, TranslationService translationService) {
        this.messageService = messageService;
        this.userService = userService;
        this.translationService = translationService;
    }


    @GetMapping("messages")
    public String messages(Model model, Principal principal, HttpServletRequest httpServletRequest,
                              @AuthenticationPrincipal OAuth2User oauth2User) {
        var messages = messageService.getPage(0, 5);
        boolean isLoggedIn = principal != null;

        Integer githubId;
        Optional<User> loggedInUser = Optional.empty();

        if (oauth2User != null) {
            githubId = oauth2User.getAttribute("id");
            loggedInUser = userService.findByUserId(githubId);
        }

        model.addAttribute("nextpage", messages.getLast().getId());
        model.addAttribute("messages", messages);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("httpServletRequest", httpServletRequest);
        loggedInUser.ifPresent(user -> model.addAttribute("loggedInUser", user));

        return "messages";
    }


    @GetMapping("nextpage")
    public String loadMore(Model model, @RequestParam(defaultValue = "1") String page, Principal principal,
                           @AuthenticationPrincipal OAuth2User oauth2User,
                           @RequestParam(defaultValue = "5") int size) {
        int p = Integer.parseInt(page);
        var messages = messageService.getPage(p, size);
        boolean isLoggedIn = principal != null;

        Integer githubId;
        Optional<User> loggedInUser = Optional.empty();

        if (oauth2User != null) {
            githubId = oauth2User.getAttribute("id");
            loggedInUser = userService.findByUserId(githubId);
        }
        model.addAttribute("messages", messages);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("nextpage", messages.getLast().getId());
        loggedInUser.ifPresent(user -> model.addAttribute("loggedInUser", user));
        return "nextpage.html";
    }

    @GetMapping("translation/{messageId}")
    public String translateMessage(@PathVariable Long messageId, Model model, HttpServletRequest httpServletRequest) {
        var messageOptional = messageService.findById(messageId);
        Message message = messageOptional.get();
        String translatedTitle = translationService.translateText(message.getMessageTitle());
        String translatedMessage = translationService.translateText(message.getMessageBody());
        String language = translationService.detectMessageLanguage(message.getMessageBody());
        model.addAttribute("postedLanguage", language);
        model.addAttribute("messageTitle", translatedTitle);
        model.addAttribute("messageBody", translatedMessage);
        model.addAttribute("httpServletRequest", httpServletRequest);

        return "translation";
    }


    @GetMapping("create")
    public String postMessage(Model model) {
        CreateMessageFormData formData = new CreateMessageFormData();
        model.addAttribute("formData", formData);

        return "create";
    }

    @PostMapping("create")
    public String greetingSubmit(@Valid @ModelAttribute("formData") CreateMessageFormData message,
                                 BindingResult bindingResult,
                                 Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        if (bindingResult.hasErrors()) {
            return "create";
        }
        Integer githubId = oauth2User.getAttribute("id");
        var loggedInUser = userService.findByUserId(githubId);

        messageService.saveMessage(message.toEntity(loggedInUser.orElse(null)));

        return "redirect:/messages/messages";
    }


    @GetMapping("update/{messageId}")
    public String updateMessage(@PathVariable Long messageId, Model model, HttpServletRequest httpServletRequest, @AuthenticationPrincipal OAuth2User oauth2User) {
        Message message = messageService.findById(messageId).get();

        String redirectUrl = redirectIfNotOwnerOrAdmin(oauth2User, message);
        if (redirectUrl != null) return redirectUrl;

        CreateMessageFormData formData = new CreateMessageFormData(message.getMessageTitle(), message.getMessageBody(), message.isPublic());
        model.addAttribute("formData", formData);
        model.addAttribute("originalMessage", message);
        model.addAttribute("messageId", messageId);
        model.addAttribute("httpServletRequest", httpServletRequest);
        return "update";
    }

    private String redirectIfNotOwnerOrAdmin(OAuth2User oauth2User, Message message) {
        if (oauth2User == null) {
            return "redirect:/messages/messages";
        }

        Integer githubId = oauth2User.getAttribute("id");
        Optional<User> loggedInUser = userService.findByUserId(githubId);

        if (!Objects.equals(message.getUser().getGitId(), githubId) && (loggedInUser.isEmpty() || !loggedInUser.get().getRole().equals("ROLE_ADMIN"))) {
            return "redirect:/messages/messages";
        }
        return null;
    }

    @PostMapping("update/{messageId}")
    public String greetingSubmit(@PathVariable Long messageId, @Valid @ModelAttribute("formData") CreateMessageFormData message,
                                 BindingResult bindingResult, @AuthenticationPrincipal OAuth2User oauth2User,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return "update";
        }

        Message originalMessage = messageService.findById(messageId).get();
        originalMessage.setMessageTitle(message.getMessageTitle());
        originalMessage.setMessageBody(message.getMessageBody());
        originalMessage.setPublic(message.isMakePublic());

        messageService.updateMessage(messageId, originalMessage);

        return "redirect:/messages/messages";
    }

    @GetMapping("/web/messages")
    public String messagesPage(@RequestParam(name = "lang", defaultValue = "en") String lang, Model model) {
        String logoutButtonText;
        if ("en".equals(lang)) {
            logoutButtonText = "Logout";
        } else if ("sv".equals(lang)) {
            logoutButtonText = "Logga ut";
        } else {
            logoutButtonText = "Logout";
        }
        model.addAttribute("logoutButtonText", logoutButtonText);

        return "messages";
    }


    @GetMapping("userMessages")
    public String getUserMessages(@RequestParam Long userId, Model model, HttpServletRequest httpServletRequest) {
        var userMessages = messageService.fidAllByUserId(userId);

        model.addAttribute("userMessages", userMessages);
        model.addAttribute("httpServletRequest", httpServletRequest);

        return "userMessages";
    }

}
