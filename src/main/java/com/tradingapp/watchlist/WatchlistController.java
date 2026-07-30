package com.tradingapp.watchlist;

import com.tradingapp.watchlist.dto.WatchlistRequest;
import com.tradingapp.watchlist.dto.WatchlistResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<WatchlistResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return watchlistService.list(principal.getUsername());
    }

    @PostMapping
    public ResponseEntity<WatchlistResponse> add(@AuthenticationPrincipal UserDetails principal,
                                                 @Valid @RequestBody WatchlistRequest request) {
        WatchlistResponse created = watchlistService.add(principal.getUsername(), request.ticker());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{ticker}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UserDetails principal, @PathVariable String ticker) {
        watchlistService.remove(principal.getUsername(), ticker);
    }
}
