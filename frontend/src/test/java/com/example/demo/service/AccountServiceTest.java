package com.example.demo.service;

import com.example.demo.dto.AccountForm;
import com.example.demo.entity.Account;
import com.example.demo.mapper.AccountMapper;
import com.example.demo.repository.AcCategoryRepository;
import com.example.demo.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String ALICE = "alice";
    private static final String BOB = "bob";

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AcCategoryRepository categoryRepository;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AccountService accountService;

    private static Account accountOf(Long id, String entryUser, String imagePath) {
        Account account = new Account();
        account.setAccountId(id);
        account.setEntryUser(entryUser);
        account.setEntryDatetime(LocalDateTime.of(2026, 1, 1, 10, 0));
        account.setImagePath(imagePath);
        return account;
    }

    private static AccountForm formOf(Long id, String imagePath) {
        AccountForm form = new AccountForm();
        form.setAccountId(id);
        form.setReceiptImagePath(imagePath);
        return form;
    }

    /** モックのMapperが、フォームのaccountIdと画像パスをEntityへ写すようにする */
    private void stubMapper() {
        when(accountMapper.toEntity(any(AccountForm.class))).thenAnswer(invocation -> {
            AccountForm f = invocation.getArgument(0);
            return accountOf(f.getAccountId(), null, f.getReceiptImagePath());
        });
    }

    private Account savedAccount() {
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static void assertStatus(HttpStatus expected, ResponseStatusException e) {
        assertEquals(expected.value(), e.getStatusCode().value());
    }

    // ---- 所有者チェック（取得・削除） ----

    @Test
    void findByIdAndOwner_otherUsersAccount_isNotFound() {
        when(accountRepository.findByAccountIdAndEntryUser(1L, BOB)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> accountService.findByIdAndOwner(1L, BOB));

        assertStatus(HttpStatus.NOT_FOUND, e);
    }

    @Test
    void delete_otherUsersAccount_deletesNeitherFileNorRecord() {
        when(accountRepository.findByAccountIdAndEntryUser(1L, BOB)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> accountService.delete(1L, BOB));

        verify(fileStorageService, never()).deleteFile(any());
        verify(accountRepository, never()).delete(any(Account.class));
        verify(accountRepository, never()).deleteById(any());
    }

    @Test
    void delete_ownAccount_deletesFileAndRecord() {
        Account account = accountOf(1L, ALICE, "/images/receipts/x.jpg");
        when(accountRepository.findByAccountIdAndEntryUser(1L, ALICE)).thenReturn(Optional.of(account));

        accountService.delete(1L, ALICE);

        verify(fileStorageService).deleteFile("/images/receipts/x.jpg");
        verify(accountRepository).delete(account);
    }

    // ---- 所有者チェック（更新） ----

    @Test
    void saveAccount_updatingOtherUsersAccount_isRejectedBeforeAnythingIsChanged() {
        when(accountRepository.findByAccountIdAndEntryUser(1L, BOB)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> accountService.saveAccount(formOf(1L, "/images/temp/u_a.jpg"), BOB));

        assertStatus(HttpStatus.NOT_FOUND, e);
        verify(fileStorageService, never()).moveToReceipts(any(), any(), any());
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveAccount_updatingOwnAccount_keepsEntryInfoAndSetsUpdateInfo() {
        Account existing = accountOf(1L, ALICE, "/images/receipts/x.jpg");
        when(accountRepository.findByAccountIdAndEntryUser(1L, ALICE)).thenReturn(Optional.of(existing));
        stubMapper();

        accountService.saveAccount(formOf(1L, "/images/receipts/x.jpg"), ALICE);

        Account saved = savedAccount();
        assertEquals(ALICE, saved.getEntryUser());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), saved.getEntryDatetime());
        assertEquals(ALICE, saved.getUpdateUser());
        assertNotNull(saved.getUpdateDatetime());
        assertEquals("/images/receipts/x.jpg", saved.getImagePath());
        verify(fileStorageService, never()).moveToReceipts(any(), any(), any());
    }

    // ---- 画像パスの検証 ----

    @Test
    void saveAccount_newAccountWithTempImage_movesImageAndSavesFinalPath() {
        when(fileStorageService.moveToReceipts(eq("/images/temp/u_a.jpg"), eq(ALICE), any(LocalDate.class)))
                .thenReturn("/images/receipts/u_a.jpg");
        stubMapper();

        accountService.saveAccount(formOf(null, "/images/temp/u_a.jpg"), ALICE);

        Account saved = savedAccount();
        assertEquals("/images/receipts/u_a.jpg", saved.getImagePath());
        assertEquals(ALICE, saved.getEntryUser());
        assertNull(saved.getUpdateUser());
    }

    @Test
    void saveAccount_blankImagePath_savesWithoutImage() {
        stubMapper();

        accountService.saveAccount(formOf(null, "  "), ALICE);

        assertNull(savedAccount().getImagePath());
        verify(fileStorageService, never()).moveToReceipts(any(), any(), any());
    }

    @Test
    void saveAccount_arbitraryImagePath_isRejected() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> accountService.saveAccount(formOf(null, "/images/../../etc/passwd"), ALICE));

        assertStatus(HttpStatus.BAD_REQUEST, e);
        verify(fileStorageService, never()).moveToReceipts(any(), any(), any());
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveAccount_pointingToAnotherReceiptImage_isRejected() {
        Account existing = accountOf(1L, ALICE, "/images/receipts/x.jpg");
        when(accountRepository.findByAccountIdAndEntryUser(1L, ALICE)).thenReturn(Optional.of(existing));

        // 自分のレコードでも、他の（他人の）画像を指すパスへは差し替えられない
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> accountService.saveAccount(formOf(1L, "/images/receipts/other-users.jpg"), ALICE));

        assertStatus(HttpStatus.BAD_REQUEST, e);
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveAccount_storageRejectsTempPath_isBadRequest() {
        when(fileStorageService.moveToReceipts(eq("/images/temp/../x.jpg"), eq(ALICE), any(LocalDate.class)))
                .thenThrow(new IllegalArgumentException("不正なファイル名です"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> accountService.saveAccount(formOf(null, "/images/temp/../x.jpg"), ALICE));

        assertStatus(HttpStatus.BAD_REQUEST, e);
        verify(accountRepository, never()).saveAndFlush(any());
    }

    // ---- DB登録に失敗した場合の後始末（転送済み画像の削除） ----

    @Test
    void saveAccount_dbFailureAfterImageMove_deletesMovedImage() {
        when(fileStorageService.moveToReceipts(eq("/images/temp/u_a.jpg"), eq(ALICE), any(LocalDate.class)))
                .thenReturn("/images/receipts/alice/2026/10/u_a.jpg");
        stubMapper();
        when(accountRepository.saveAndFlush(any(Account.class))).thenThrow(new IllegalStateException("db error"));

        assertThrows(IllegalStateException.class,
                () -> accountService.saveAccount(formOf(null, "/images/temp/u_a.jpg"), ALICE));

        verify(fileStorageService).deleteFile("/images/receipts/alice/2026/10/u_a.jpg");
    }

    @Test
    void saveAccount_dbFailureWithoutImageMove_doesNotDeleteExistingImage() {
        Account existing = accountOf(1L, ALICE, "/images/receipts/x.jpg");
        when(accountRepository.findByAccountIdAndEntryUser(1L, ALICE)).thenReturn(Optional.of(existing));
        stubMapper();
        when(accountRepository.saveAndFlush(any(Account.class))).thenThrow(new IllegalStateException("db error"));

        assertThrows(IllegalStateException.class,
                () -> accountService.saveAccount(formOf(1L, "/images/receipts/x.jpg"), ALICE));

        // 画像を転送していない（元の画像のまま）ので、既存の画像を消してはいけない
        verify(fileStorageService, never()).deleteFile(any());
    }

    @Test
    void saveAccount_dbAndImageDeleteBothFail_keepsOriginalException() {
        when(fileStorageService.moveToReceipts(eq("/images/temp/u_a.jpg"), eq(ALICE), any(LocalDate.class)))
                .thenReturn("/images/receipts/alice/2026/10/u_a.jpg");
        stubMapper();
        when(accountRepository.saveAndFlush(any(Account.class))).thenThrow(new IllegalStateException("db error"));
        doThrow(new RuntimeException("storage down")).when(fileStorageService).deleteFile(any());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> accountService.saveAccount(formOf(null, "/images/temp/u_a.jpg"), ALICE));

        // 画像の削除に失敗しても、元のDBエラーが呼び出し元へ伝わる
        assertEquals("db error", thrown.getMessage());
    }
}
