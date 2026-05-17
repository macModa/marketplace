package com.marketplace.mapper;

import com.marketplace.dto.OrderDto;
import com.marketplace.dto.OrderLineDto;
import com.marketplace.entity.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }

        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setStatut(order.getStatut());
        dto.setTotal(order.getTotal());
        dto.setDateCreation(order.getDateCreation());
        dto.setDateModification(order.getDateModification());
        dto.setDeliveryToken(order.getDeliveryToken());
        dto.setTrackingNumber(order.getTrackingNumber()); // ✅ ajouté

        if (order.getPaymentMethod() != null) {
            dto.setPaymentMethod(order.getPaymentMethod().name());
        }
        if (order.getPaymentStatus() != null) {
            dto.setPaymentStatus(order.getPaymentStatus().name());
        }

        if (order.getClient() != null) {
            dto.setClientId(order.getClient().getId());
            dto.setClientNom(order.getClient().getNom());
        }

        if (order.getOrderLines() != null) {
            List<OrderLineDto> lines = order.getOrderLines().stream().map(line -> {
                OrderLineDto lineDto = new OrderLineDto();
                lineDto.setId(line.getId());
                lineDto.setQuantite(line.getQuantite());
                lineDto.setPrixUnitaire(line.getPrixUnitaire());

                if (line.getPrixUnitaire() != null && line.getQuantite() != null) {
                    lineDto.setSubtotal(line.getPrixUnitaire().multiply(BigDecimal.valueOf(line.getQuantite())));
                }

                if (line.getProduct() != null) {
                    lineDto.setProductId(line.getProduct().getId());
                    lineDto.setProductNom(line.getProduct().getNom());
                }
                return lineDto;
            }).collect(Collectors.toList());
            dto.setOrderLines(lines);
        }
        return dto;
    }
}
