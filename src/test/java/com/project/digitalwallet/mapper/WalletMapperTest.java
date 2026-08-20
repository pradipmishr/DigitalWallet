package com.project.digitalwallet.mapper;

import com.project.digitalwallet.common.enums.WalletStatus;
import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletMapperTest {

    @Test
    void toWalletDto_shouldReturnNull_whenWalletIsNull() {
        WalletDto result = WalletMapper.toWalletDto(null);
        assertNull(result);
    }

    @Test
    void toWalletDto_shouldMapWalletWithoutUser() {
        Wallet wallet = mock(Wallet.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getWalletNumber()).thenReturn("WALLET001");
        when(wallet.getBalance()).thenReturn(new BigDecimal("1000.00"));
        when(wallet.getStatus()).thenReturn(WalletStatus.ACTIVE);
        when(wallet.getUser()).thenReturn(null);

        WalletDto result = WalletMapper.toWalletDto(wallet);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("WALLET001", result.getWalletNumber());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        assertEquals(WalletStatus.ACTIVE, result.getStatus());

        assertNull(result.getUserId());
        assertNull(result.getUserName());
        assertNull(result.getUserPhoneNumber());
    }

    @Test
    void toWalletDto_shouldMapWalletWithUser() {
        Wallet wallet = mock(Wallet.class);
        User user = mock(User.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getWalletNumber()).thenReturn("WALLET001");
        when(wallet.getBalance()).thenReturn(new BigDecimal("1000.00"));
        when(wallet.getStatus()).thenReturn(WalletStatus.ACTIVE);
        when(wallet.getUser()).thenReturn(user);

        when(user.getId()).thenReturn(10L);
        when(user.getFirstName()).thenReturn("John");
        when(user.getLastName()).thenReturn("Doe");
        when(user.getPhoneNumber()).thenReturn("9800000000");

        WalletDto result = WalletMapper.toWalletDto(wallet);

        assertNotNull(result);

        assertEquals(1L, result.getId());
        assertEquals("WALLET001", result.getWalletNumber());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        assertEquals(WalletStatus.ACTIVE, result.getStatus());

        assertEquals(10L, result.getUserId());
        assertEquals("John Doe", result.getUserName());
        assertEquals("9800000000", result.getUserPhoneNumber());
    }

    @Test
    void toWalletEntity_shouldReturnNull_whenWalletDtoIsNull() {
        Wallet result = WalletMapper.toWalletEntity(null);
        assertNull(result);
    }

    @Test
    void toWalletEntity_shouldMapWalletDtoToEntity() {
        WalletDto walletDto = new WalletDto();

        walletDto.setId(1L);
        walletDto.setWalletNumber("WALLET001");
        walletDto.setBalance(new BigDecimal("1000.00"));
        walletDto.setStatus(WalletStatus.ACTIVE);

        Wallet result = WalletMapper.toWalletEntity(walletDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        assertEquals(WalletStatus.ACTIVE, result.getStatus());

        // walletNumber is NOT mapped in toWalletEntity()
        assertNull(result.getWalletNumber());

        // User is also NOT mapped in toWalletEntity()
        assertNull(result.getUser());
    }
}