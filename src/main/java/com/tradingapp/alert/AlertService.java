package com.tradingapp.alert;

import com.tradingapp.alert.dto.AlertRequest;
import com.tradingapp.alert.dto.AlertResponse;
import com.tradingapp.common.NotFoundException;
import com.tradingapp.common.Tickers;
import com.tradingapp.user.User;
import com.tradingapp.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every operation is scoped to the authenticated user. */
@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public AlertService(AlertRepository alertRepository, UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> list(String userEmail) {
        User user = requireUser(userEmail);
        return alertRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId()).stream()
                .map(AlertResponse::from)
                .toList();
    }

    @Transactional
    public AlertResponse create(String userEmail, AlertRequest request) {
        User user = requireUser(userEmail);
        Alert alert = new Alert(
                user,
                Tickers.normalize(request.ticker()),
                request.targetPrice(),
                request.direction());
        return AlertResponse.from(alertRepository.save(alert));
    }

    @Transactional
    public void delete(String userEmail, Long alertId) {
        User user = requireUser(userEmail);
        Alert alert = alertRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new NotFoundException("alert " + alertId + " not found"));
        alertRepository.delete(alert);
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }
}
