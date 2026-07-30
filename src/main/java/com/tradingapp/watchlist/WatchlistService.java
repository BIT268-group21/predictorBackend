package com.tradingapp.watchlist;

import com.tradingapp.common.ApiException;
import com.tradingapp.common.NotFoundException;
import com.tradingapp.common.Tickers;
import com.tradingapp.user.User;
import com.tradingapp.user.UserRepository;
import com.tradingapp.watchlist.dto.WatchlistResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every operation is scoped to the authenticated user. */
@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;

    public WatchlistService(WatchlistRepository watchlistRepository, UserRepository userRepository) {
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WatchlistResponse> list(String userEmail) {
        User user = requireUser(userEmail);
        return watchlistRepository.findByUserIdOrderByCreatedAtAscIdAsc(user.getId()).stream()
                .map(WatchlistResponse::from)
                .toList();
    }

    @Transactional
    public WatchlistResponse add(String userEmail, String rawTicker) {
        User user = requireUser(userEmail);
        String ticker = Tickers.normalize(rawTicker);
        if (watchlistRepository.existsByUserIdAndTicker(user.getId(), ticker)) {
            throw new ApiException(HttpStatus.CONFLICT, ticker + " is already on your watchlist");
        }
        WatchlistItem saved = watchlistRepository.save(new WatchlistItem(user, ticker));
        return WatchlistResponse.from(saved);
    }

    @Transactional
    public void remove(String userEmail, String rawTicker) {
        User user = requireUser(userEmail);
        String ticker = Tickers.normalize(rawTicker);
        WatchlistItem item = watchlistRepository.findByUserIdAndTicker(user.getId(), ticker)
                .orElseThrow(() -> new NotFoundException(ticker + " is not on your watchlist"));
        watchlistRepository.delete(item);
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }
}
