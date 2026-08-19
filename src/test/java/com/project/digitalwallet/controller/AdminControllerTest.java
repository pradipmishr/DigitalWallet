package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.enums.KycStatus;
import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.AdminService;
import com.project.digitalwallet.service.KycService;
import com.project.digitalwallet.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private KycService kycService;

    @InjectMocks
    private AdminController adminController;


    // =========================================================
    // Dashboard
    // =========================================================

    @Test
    void getDashboardStats_shouldReturnDashboardStats() {

        AdminDashboardStatsDto stats = mock(AdminDashboardStatsDto.class);

        when(adminService.getDashboardStats())
                .thenReturn(stats);

        ResponseWrapper<AdminDashboardStatsDto> response =
                adminController.getDashboardStats();

        assertNotNull(response);
        assertEquals(stats, response.getData());
        assertEquals("Dashboard stats", response.getMessage());
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(adminService).getDashboardStats();
    }


    // =========================================================
    // Audit Logs
    // =========================================================

    @Test
    void getAllAuditLogs_whenActionProvided_shouldCallGetAuditLogsByAction() {

        Page<AuditLogDto> page =
                new PageImpl<>(List.of(mock(AuditLogDto.class)));

        when(adminService.getAuditLogsByAction("LOGIN", 1, 20))
                .thenReturn(page);

        ResponseWrapper<Page<AuditLogDto>> response =
                adminController.getAllAuditLogs(1, 20, "LOGIN");

        assertNotNull(response);
        assertEquals(page, response.getData());
        assertEquals("Audit logs", response.getMessage());
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(adminService)
                .getAuditLogsByAction("LOGIN", 1, 20);

        verify(adminService, never())
                .getAllAuditLogs(anyInt(), anyInt());
    }


    @Test
    void getAllAuditLogs_whenActionIsNull_shouldReturnAllAuditLogs() {

        Page<AuditLogDto> page =
                new PageImpl<>(List.of(mock(AuditLogDto.class)));

        when(adminService.getAllAuditLogs(0, 10))
                .thenReturn(page);

        ResponseWrapper<Page<AuditLogDto>> response =
                adminController.getAllAuditLogs(0, 10, null);

        assertNotNull(response);
        assertEquals(page, response.getData());
        assertEquals("All audit logs", response.getMessage());
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(adminService)
                .getAllAuditLogs(0, 10);

        verify(adminService, never())
                .getAuditLogsByAction(anyString(), anyInt(), anyInt());
    }


    @Test
    void getAllAuditLogs_whenActionIsBlank_shouldReturnAllAuditLogs() {

        Page<AuditLogDto> page =
                new PageImpl<>(List.of(mock(AuditLogDto.class)));

        when(adminService.getAllAuditLogs(0, 10))
                .thenReturn(page);

        ResponseWrapper<Page<AuditLogDto>> response =
                adminController.getAllAuditLogs(0, 10, "   ");

        assertNotNull(response);
        assertEquals(page, response.getData());

        verify(adminService)
                .getAllAuditLogs(0, 10);

        verify(adminService, never())
                .getAuditLogsByAction(anyString(), anyInt(), anyInt());
    }


    // =========================================================
    // Freeze Wallet
    // =========================================================

    @Test
    void freezeWallet_shouldCallServiceAndReturnWallet() {

        WalletDto wallet = mock(WalletDto.class);

        when(adminService.freezeWalletByPhoneNumber("9800000000"))
                .thenReturn(wallet);

        ResponseWrapper<WalletDto> response =
                adminController.freezeWallet("9800000000");

        assertNotNull(response);
        assertEquals(wallet, response.getData());
        assertEquals("Wallet freezed", response.getMessage());
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(adminService)
                .freezeWalletByPhoneNumber("9800000000");
    }


    // =========================================================
    // Unfreeze Wallet
    // =========================================================

    @Test
    void unfreezeWallet_shouldCallServiceAndReturnWallet() {

        WalletDto wallet = mock(WalletDto.class);

        when(adminService.unfreezeWalletByPhoneNumber("9800000000"))
                .thenReturn(wallet);

        ResponseWrapper<WalletDto> response =
                adminController.unfreezeWallet("9800000000");

        assertNotNull(response);
        assertEquals(wallet, response.getData());
        assertEquals("Wallet Unfreezed", response.getMessage());
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(adminService)
                .unfreezeWalletByPhoneNumber("9800000000");
    }


    // =========================================================
    // Get Users
    // =========================================================

    @Test
    void getUsers_whenSearchProvided_shouldSearchUsers() {

        Page<AdminUserResponseDto> page =
                new PageImpl<>(List.of(mock(AdminUserResponseDto.class)));

        when(adminService.searchUsers("john", 0, 10))
                .thenReturn(page);

        ResponseWrapper<Page<AdminUserResponseDto>> response =
                adminController.getUsers("john", 0, 10);

        assertNotNull(response);
        assertEquals(page, response.getData());
        assertEquals("Users retrieved successfully", response.getMessage());
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(adminService)
                .searchUsers("john", 0, 10);

        verify(adminService, never())
                .getAllUsers(anyInt(), anyInt());
    }


    @Test
    void getUsers_whenSearchIsNull_shouldReturnAllUsers() {

        Page<AdminUserResponseDto> page =
                new PageImpl<>(List.of(mock(AdminUserResponseDto.class)));

        when(adminService.getAllUsers(0, 10))
                .thenReturn(page);

        ResponseWrapper<Page<AdminUserResponseDto>> response =
                adminController.getUsers(null, 0, 10);

        assertNotNull(response);
        assertEquals(page, response.getData());

        verify(adminService)
                .getAllUsers(0, 10);

        verify(adminService, never())
                .searchUsers(anyString(), anyInt(), anyInt());
    }


    @Test
    void getUsers_whenSearchIsBlank_shouldReturnAllUsers() {

        Page<AdminUserResponseDto> page =
                new PageImpl<>(List.of(mock(AdminUserResponseDto.class)));

        when(adminService.getAllUsers(1, 20))
                .thenReturn(page);

        ResponseWrapper<Page<AdminUserResponseDto>> response =
                adminController.getUsers("   ", 1, 20);

        assertNotNull(response);
        assertEquals(page, response.getData());

        verify(adminService)
                .getAllUsers(1, 20);

        verify(adminService, never())
                .searchUsers(anyString(), anyInt(), anyInt());
    }


    // =========================================================
    // Reset PIN
    // =========================================================

    @Test
    void resetUserPin_shouldCallService() {

        AdminResetPinRequest request =
                mock(AdminResetPinRequest.class);

        when(request.getPhoneNumber())
                .thenReturn("9800000000");

        ResponseWrapper<String> response =
                adminController.resetUserPin(request);

        assertNotNull(response);

        assertEquals(
                "Transaction PIN reset successfully for phone number: 9800000000",
                response.getData()
        );

        assertEquals("SUCCESS", response.getMessage());
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(adminService)
                .resetUserTransactionPin(request);
    }


    // =========================================================
    // Change Password
    // =========================================================

    @Test
    void changeUserPassword_shouldCallService() {

        AdminChangePasswordRequest request =
                mock(AdminChangePasswordRequest.class);

        when(request.getPhoneNumber())
                .thenReturn("9800000000");

        ResponseWrapper<String> response =
                adminController.changeUserPassword(request);

        assertNotNull(response);

        assertEquals(
                "Password changed successfully for user with phone number: 9800000000",
                response.getData()
        );

        assertEquals("SUCCESS", response.getMessage());
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(adminService)
                .changeUserPassword(request);
    }


    // =========================================================
    // Search Transactions
    // =========================================================

    @Test
    void searchTransactions_shouldReturnTransactionResults() {

        AdminTransactionSearchRequest request =
                mock(AdminTransactionSearchRequest.class);

        Page<TransactionDto> page =
                new PageImpl<>(List.of(mock(TransactionDto.class)));

        when(transactionService.searchTransactionsForAdmin(request))
                .thenReturn(page);

        ResponseWrapper<Page<TransactionDto>> response =
                adminController.searchTransactions(request);

        assertNotNull(response);
        assertEquals(page, response.getData());
        assertEquals(
                "Transactions fetched successfully",
                response.getMessage()
        );
        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());

        verify(transactionService)
                .searchTransactionsForAdmin(request);
    }

//    @Test
//    void searchTransactions_shouldReturnTransactionResult() {
//        AdminTransactionSearchRequest request = mock(AdminTransactionSearchRequest.class);
//        Page<TransactionDto> page = new PageImpl<>(List.of(mock(TransactionDto.class)));
//
//        when(transactionService.searchTransactionsForAdmin(request)).thenReturn(page);
//
//        ResponseWrapper<Page<TransactionDto>> response = adminController.searchTransactions(request);
//
//        assertNotNull(response);
//        assertEquals(page,response.getData());
//        assertEquals("Transactions fetched successfully",response.getMessage());
//        assertEquals(200, response.getStatus());
//        assertTrue(response.isSuccess());
//
//        verify(transactionService).searchTransactionsForAdmin(request);
//    }

    // =========================================================
    // Reverse Transaction
    // =========================================================

//    @Test
//    void reverseTransaction_shouldCallServiceAndReturnResult() {
//
//        UserPrincipal adminPrincipal =
//                mock(UserPrincipal.class);
//
//        AdminReverseTransactionRequest request =
//                mock(AdminReverseTransactionRequest.class);
//
//        UserDto adminUserDto =
//                mock(UserDto.class);
//
//        TransactionDto transactionDto =
//                mock(TransactionDto.class);
//
//        when(transactionService.reverseTransaction(
//                adminUserDto,
//                request
//        )).thenReturn(transactionDto);
//
//        /*
//         * UserMapper.toUserDto(...) is static.
//         * Mock it only for this test.
//         */
//        try (MockedStatic<UserMapper> userMapper =
//                     Mockito.mockStatic(UserMapper.class)) {
//
//            userMapper.when(() ->
//                    UserMapper.toUserDto(adminPrincipal.getUser())
//            ).thenReturn(adminUserDto);
//
//            ResponseWrapper<TransactionDto> response =
//                    adminController.reverseTransaction(
//                            adminPrincipal,
//                            request
//                    );
//
//            assertNotNull(response);
//            assertEquals(transactionDto, response.getData());
//            assertEquals(
//                    "Transaction reversed successfully.",
//                    response.getMessage()
//            );
//            assertEquals(200, response.getStatus());
//            assertTrue(response.isSuccess());
//
//            verify(transactionService)
//                    .reverseTransaction(adminUserDto, request);
//        }
//    }
    @Test
    void reverseTransaction_shouldCallServiceAndReturnResult() {
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        AdminReverseTransactionRequest request = mock(AdminReverseTransactionRequest.class);
        UserDto userDto = mock(UserDto.class);
        TransactionDto transactionDto = mock(TransactionDto.class);

        when(transactionService.reverseTransaction(userDto, request)).thenReturn(transactionDto);
        try (MockedStatic<UserMapper> userMapper = Mockito.mockStatic(UserMapper.class)) {

            userMapper.when(() -> UserMapper.toUserDto(userPrincipal.getUser())).thenReturn(userDto);

            ResponseWrapper<TransactionDto> response = adminController.reverseTransaction(userPrincipal, request);

            assertNotNull(response);
            assertEquals(transactionDto, response.getData());
            assertEquals("Transaction reversed successfully.", response.getMessage());
            assertEquals(200, response.getStatus());
            assertTrue(response.isSuccess());

            verify(transactionService).reverseTransaction(userDto, request);

        }
    }




        // =========================================================
    // KYC - Get All
    // =========================================================

    @Test
    void getAllKycs_shouldUseDefaultPaginationAndSorting() {

        Page<KycStatusResponse> page =
                new PageImpl<>(List.of(mock(KycStatusResponse.class)));

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(kycService.getAllKycs(isNull(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ResponseWrapper<Page<KycStatusResponse>>> response =
                adminController.getAllKycs(
                        null,
                        0,
                        10,
                        "createdAt",
                        "DESC"
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(page, response.getBody().getData());
        assertEquals(
                "KYC list retrieved successfully.",
                response.getBody().getMessage()
        );

        verify(kycService)
                .getAllKycs(isNull(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());

        Sort.Order order =
                pageable.getSort().getOrderFor("createdAt");

        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }


    @Test
    void getAllKycs_whenSortDirectionAsc_shouldUseAscendingSort() {

        Page<KycStatusResponse> page =
                new PageImpl<>(List.of(mock(KycStatusResponse.class)));

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(kycService.getAllKycs(
                eq(KycStatus.PENDING),
                any(Pageable.class)
        )).thenReturn(page);

        ResponseEntity<ResponseWrapper<Page<KycStatusResponse>>> response =
                adminController.getAllKycs(
                        KycStatus.PENDING,
                        2,
                        25,
                        "updatedAt",
                        "ASC"
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(page, response.getBody().getData());

        verify(kycService)
                .getAllKycs(
                        eq(KycStatus.PENDING),
                        pageableCaptor.capture()
                );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());

        Sort.Order order =
                pageable.getSort().getOrderFor("updatedAt");

        assertNotNull(order);
        assertEquals(
                Sort.Direction.ASC,
                order.getDirection()
        );
    }


//    @Test
//    void getAllKycs_whenSortDirectionInvalid_shouldUseDesc() {
//
//        Page<KycStatusResponse> page =
//                new PageImpl<>();
//
//        ArgumentCaptor<Pageable> pageableCaptor =
//                ArgumentCaptor.forClass(Pageable.class);
//
//        when(kycService.getAllKycs(
//                isNull(),
//                any(Pageable.class)
//        )).thenReturn(page);
//
//        adminController.getAllKycs(
//                null,
//                0,
//                10,
//                "createdAt",
//                "INVALID"
//        );
//
//        verify(kycService)
//                .getAllKycs(
//                        isNull(),
//                        pageableCaptor.capture()
//                );
//
//        Pageable pageable = pageableCaptor.getValue();
//
//        Sort.Order order =
//                pageable.getSort().getOrderFor("createdAt");
//
//        assertNotNull(order);
//        assertEquals(
//                Sort.Direction.DESC,
//                order.getDirection()
//        );
//    }
//
//
    @Test
    void getAllKycs_whenSortByBlank_shouldUseCreatedAt() {

        Page<KycStatusResponse> page =
                new PageImpl<>(List.of());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(kycService.getAllKycs(
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        adminController.getAllKycs(
                null,
                0,
                10,
                "   ",
                "DESC"
        );

        verify(kycService)
                .getAllKycs(
                        isNull(),
                        pageableCaptor.capture()
                );

        Pageable pageable = pageableCaptor.getValue();

        assertNotNull(
                pageable.getSort().getOrderFor("createdAt")
        );
    }


    // =========================================================
    // KYC - Get By ID
    // =========================================================

//    @Test
//    void getKycById_shouldReturnKyc() {
//
//        KycStatusResponse kyc =
//                mock(KycStatusResponse.class);
//
//        when(kycService.getKycById(100L))
//                .thenReturn(kyc);
//
//        ResponseEntity<ResponseWrapper<KycStatusResponse>> response =
//                adminController.getKycById(100L);
//
//        assertEquals(200, response.getStatusCode().value());
//        assertNotNull(response.getBody());
//
//        assertEquals(kyc, response.getBody().getData());
//        assertEquals(
//                "KYC record retrieved successfully.",
//                response.getBody().getMessage()
//        );
//
//        verify(kycService)
//                .getKycById(100L);
//    }

    @Test
    void getKycById_shouldReturnKyc() {
        KycStatusResponse kyc = mock(KycStatusResponse.class);
        when(kycService.getKycById(1L)).thenReturn(kyc);
        ResponseEntity<ResponseWrapper<KycStatusResponse>> response = adminController.getKycById(1L);
        assertEquals(kyc, response.getBody().getData());
        assertEquals("KYC record retrieved successfully.",response.getBody().getMessage());
        assertEquals(200,response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());

        verify(kycService).getKycById(1L);
    }



    // =========================================================
    // KYC - Get By User ID
    // =========================================================

//    @Test
//    void getKycByUserIdForAdmin_shouldReturnKyc() {
//
//        KycStatusResponse kyc =
//                mock(KycStatusResponse.class);
//
//        when(kycService.getKycByUserId(50L))
//                .thenReturn(kyc);
//
//        ResponseEntity<ResponseWrapper<KycStatusResponse>> response =
//                adminController.getKycByUserIdForAdmin(50L);
//
//        assertEquals(200, response.getStatusCode().value());
//        assertNotNull(response.getBody());
//
//        assertEquals(kyc, response.getBody().getData());
//
//        assertEquals(
//                "KYC record for target user retrieved successfully.",
//                response.getBody().getMessage()
//        );
//
//        verify(kycService)
//                .getKycByUserId(50L);
//    }

    @Test
    void getgetKycByUserIdForAdmin_shouldGetKyc(){
        KycStatusResponse kyc = mock(KycStatusResponse.class);
        when(kycService.getKycByUserId(1L)).thenReturn(kyc);
        ResponseEntity<ResponseWrapper<KycStatusResponse>> response = adminController.getKycByUserIdForAdmin(1L);

        assertEquals(kyc,response.getBody().getData());
        assertEquals("KYC record for target user retrieved successfully.",response.getBody().getMessage());
        assertNotNull(response.getBody());
        assertEquals(200,response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());

        verify(kycService).getKycByUserId(1L);
    }


    // =========================================================
    // KYC - Review
    // =========================================================

//    @Test
//    void reviewKyc_shouldCallServiceAndReturnResponse() {
//
//        ReviewKycRequest request =
//                mock(ReviewKycRequest.class);
//
//        KycStatusResponse kyc =
//                mock(KycStatusResponse.class);
//
//        when(kycService.reviewKyc(10L, request))
//                .thenReturn(kyc);
//
//        ResponseEntity<ResponseWrapper<KycStatusResponse>> response =
//                adminController.reviewKyc(10L, request);
//
//        assertEquals(200, response.getStatusCode().value());
//        assertNotNull(response.getBody());
//
//        assertEquals(kyc, response.getBody().getData());
//
//        assertEquals(
//                "KYC application reviewed successfully.",
//                response.getBody().getMessage()
//        );
//
//        verify(kycService)
//                .reviewKyc(10L, request);
//    }
    @Test
    void reviewKyc_shouldCallServiceAndReturnResponse() {
        ReviewKycRequest request = mock(ReviewKycRequest.class);
        KycStatusResponse kyc = mock(KycStatusResponse.class);

        when(kycService.reviewKyc(1L,request)).thenReturn(kyc);
        ResponseEntity<ResponseWrapper<KycStatusResponse>> response = adminController.reviewKyc(1L,request);

        assertEquals(kyc, response.getBody().getData());
        assertEquals(200, response.getStatusCode().value());
        assertEquals("KYC application reviewed successfully.", response.getBody().getMessage());
        assertTrue(response.getBody().isSuccess());

        verify(kycService).reviewKyc(1L,request);
    }

}