package br.com.ifood.chronos.restaurant.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.ifood.chronos.restaurant.client.ChronosSchedulerClient;
import br.com.ifood.chronos.restaurant.client.exception.AbstractClientException;
import br.com.ifood.chronos.restaurant.model.Unavailability;

@Controller
@RequestMapping("/")
public class HomeController {
	
	private static final String ERROR_MESSAGES = "errorMessages";
	
	private final ChronosSchedulerClient chronosSchedulerClient;
	
	public HomeController(ChronosSchedulerClient chronosSchedulerClient) {
		this.chronosSchedulerClient = chronosSchedulerClient;
	}

	@RequestMapping(method=GET)
	String home(Model model) {
		List<Unavailability> unavailabilities = chronosSchedulerClient.getUnavailabilities();
		model.addAttribute("localDateTimeFormat", DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
		model.addAttribute("unavailabilities", unavailabilities);
		return "home";
	}
	
	@RequestMapping(value="/unavailability", method=GET)
	String unavailability() {
		return "unavailability";
	}
	
	@RequestMapping(value="/unavailability", method=POST)
	String saveUnavailability(Model model, @ModelAttribute Unavailability unavailability) {
		try {
			chronosSchedulerClient.createUnavailability(unavailability);
		} catch (AbstractClientException ex) {
			model.addAttribute(ERROR_MESSAGES, ex.getMessages());
			return unavailability();
		}
		return "redirect:/";
	}
	
	@RequestMapping(value="/unavailability/delete", method=POST)
	String deleteUnavailability(Model model, @ModelAttribute Unavailability unavailability, RedirectAttributes redirectAttrs) {
		try {
			chronosSchedulerClient.deleteUnavailability(unavailability);
		} catch (AbstractClientException ex) {
			redirectAttrs.addFlashAttribute(ERROR_MESSAGES, ex.getMessages());
		}
		return "redirect:/";
	}
}