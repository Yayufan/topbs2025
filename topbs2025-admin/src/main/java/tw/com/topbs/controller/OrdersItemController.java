package tw.com.topbs.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "訂單明細API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/orders-item")
public class OrdersItemController {

	
}
