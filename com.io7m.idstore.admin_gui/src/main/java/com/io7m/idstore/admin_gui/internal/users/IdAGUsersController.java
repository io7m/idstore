/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.idstore.admin_gui.internal.users;

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGCSS;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientService;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientStatus;
import com.io7m.idstore.admin_gui.internal.main.IdAGMainScreenController;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.io7m.idstore.admin_gui.internal.client.IdAGClientStatus.DISCONNECTED;
import static javafx.scene.control.SelectionMode.SINGLE;

/**
 * The user tab controller.
 */

public final class IdAGUsersController implements Initializable
{
  private final IdAGConfiguration configuration;
  private final IdServiceDirectoryType mainServices;
  private final IdAGStrings strings;
  private final IdAGClientService client;
  private final ObservableList<IdAGUser> users;
  private final ObservableList<IdEmail> userEmails;
  private IdUser user;
  private IdAGUserSearchKind searchKindAtStart;

  @FXML private Parent userTableContainer;
  @FXML private Parent userDetailContainer;
  @FXML private Button userCreate;
  @FXML private Button userDelete;
  @FXML private ListView<IdAGUser> userList;
  @FXML private ListView<IdEmail> userEmailList;
  @FXML private TextField userSearchField;
  @FXML private Button userPageNext;
  @FXML private Button userPagePrev;
  @FXML private Label userPageLabel;
  @FXML private TextField userIdField;
  @FXML private TextField userIdNameField;
  @FXML private TextField userRealNameField;
  @FXML private Button emailAdd;
  @FXML private Button emailDelete;
  @FXML private ChoiceBox<IdAGUserSearchKind> searchKind;

  /**
   * The user tab controller.
   *
   * @param inMainServices  The service directory
   * @param inConfiguration The configuration
   */

  public IdAGUsersController(
    final IdServiceDirectoryType inMainServices,
    final IdAGConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.mainServices =
      Objects.requireNonNull(inMainServices, "mainServices");
    this.strings =
      this.mainServices.requireService(IdAGStrings.class);
    this.client =
      this.mainServices.requireService(IdAGClientService.class);
    this.users =
      FXCollections.observableArrayList();
    this.userEmails =
      FXCollections.observableArrayList();
  }

  private void onClientStatusChanged(
    final IdAGClientStatus statusNew)
  {
    if (statusNew == DISCONNECTED) {
      this.users.clear();
      this.user = null;
      this.userDetailsLock();
      this.userTableControlsLock();
    }
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.searchKind.setConverter(
      new IdAGUserSearchKindStringConverter(this.strings));
    this.searchKind.setItems(
      FXCollections.observableArrayList(IdAGUserSearchKind.values())
    );
    this.searchKind.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, kindOld, kindNew) -> {
        this.onSearchKindSelected(kindNew);
      });
    this.searchKind.getSelectionModel()
      .select(IdAGUserSearchKind.BY_DETAILS);

    this.userDetailsLock();
    this.userTableControlsLock();

    this.userList.setCellFactory(new IsAGUserCellFactory(this.strings));
    this.userList.setFixedCellSize(24.0);
    this.userList.getSelectionModel().setSelectionMode(SINGLE);
    this.userList.setItems(this.users);
    this.userList.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, userOld, userNew) -> {
      this.onUserSelected(userNew);
    });

    this.userEmailList.setItems(this.userEmails);
    this.userEmailList.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, emailOld, emailNew) -> {
        this.onEmailSelected(emailNew);
      });

    this.client.status()
      .addListener((obs, statusOld, statusNew) -> {
        this.onClientStatusChanged(statusNew);
      });
  }

  private void onSearchKindSelected(
    final IdAGUserSearchKind kind)
  {
    switch (kind) {
      case BY_DETAILS -> {
        this.userSearchField.setPromptText(
          this.strings.format("users.searchPrompt.searchDetails")
        );
      }
      case BY_EMAIL -> {
        this.userSearchField.setPromptText(
          this.strings.format("users.searchPrompt.searchEmail")
        );
      }
    }
  }

  private void onEmailSelected(
    final IdEmail email)
  {
    this.emailDelete.setDisable(true);

    if (email == null) {
      return;
    }

    this.emailDelete.setDisable(this.userEmails.size() <= 1);
  }

  private void userTableControlsLock()
  {
    this.userPageNext.setDisable(true);
    this.userPagePrev.setDisable(true);
    this.userPageLabel.setText("");
  }

  private void userDetailsLock()
  {
    this.userDelete.setDisable(true);
    this.userDetailContainer.setDisable(true);
    this.userIdField.setText("");
    this.userIdNameField.setText("");
    this.userRealNameField.setText("");
    this.userEmails.clear();
    this.emailAdd.setDisable(true);
    this.emailDelete.setDisable(true);
  }

  private void userDetailsUnlock()
  {
    this.userDelete.setDisable(false);
    this.userDetailContainer.setDisable(false);
  }

  private void onUserSelected(
    final IdAGUser userNew)
  {
    this.userDetailsLock();
    if (userNew == null) {
      return;
    }

    final var future = this.client.userGet(userNew.id());
    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onUserReceived(received);
      }
    });

    this.userDetailsUnlock();
  }

  private void onUserReceived(
    final Optional<IdUser> userOpt)
  {
    Platform.runLater(() -> {
      this.userDetailsLock();

      if (userOpt.isEmpty()) {
        this.user = null;
        return;
      }

      this.user = userOpt.get();
      this.userIdField.setText(this.user.id().toString());
      this.userIdNameField.setText(this.user.idName().toString());
      this.userRealNameField.setText(this.user.realName().toString());
      this.userEmails.setAll(this.user.emails().toList());
      this.userDetailsUnlock();
      this.emailAdd.setDisable(false);
      this.emailDelete.setDisable(true);
    });
  }

  @FXML
  private void onUserCreateSelected()
  {

  }

  @FXML
  private void onUserDeleteSelected()
    throws IOException
  {
    final var controller =
      this.openUserDeleteConfirmDialog();

    if (controller.isDeleteRequested()) {
      final var future = this.client.userDelete(this.user.id());
      future.whenComplete((page, exception) -> {
        // OK
      });
    }
  }

  @FXML
  private void onUserPageNext()
  {
    switch (this.searchKindAtStart) {
      case BY_DETAILS -> {
        final var future = this.client.userSearchNext();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
      case BY_EMAIL -> {
        final var future = this.client.userSearchByEmailNext();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  @FXML
  private void onUserPagePrevious()
  {
    switch (this.searchKindAtStart) {
      case BY_DETAILS -> {
        final var future = this.client.userSearchPrevious();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
      case BY_EMAIL -> {
        final var future = this.client.userSearchByEmailPrevious();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  @FXML
  private void onUserSearchSelected()
  {
    this.searchKindAtStart = this.searchKind.getValue();

    switch (this.searchKind.getValue()) {
      case BY_DETAILS -> {
        final var future =
          this.client.userSearchBegin(
            IdTimeRange.largest(),
            IdTimeRange.largest(),
            Optional.of(this.userSearchField.getCharacters().toString())
          );

        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }

      case BY_EMAIL -> {
        final var future =
          this.client.userSearchByEmailBegin(
            IdTimeRange.largest(),
            IdTimeRange.largest(),
            this.userSearchField.getCharacters().toString()
          );

        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  private void onPageReceived(
    final IdPage<IdUserSummary> page)
  {
    Platform.runLater(() -> {
      final var pageIndex = page.pageIndex() + 1;
      final var pageCount = page.pageCount();

      this.userPagePrev.setDisable(pageIndex == 1);
      this.userPageNext.setDisable(pageIndex == pageCount);

      this.userPageLabel.setText(
        this.strings.format(
          "users.page",
          Integer.valueOf(pageIndex),
          Integer.valueOf(pageCount))
      );

      this.users.setAll(
        page.items()
          .stream()
          .map(IdAGUser::of)
          .toList()
      );
    });
  }

  @FXML
  private void onEmailAddSelected()
    throws IOException
  {
    final var controller =
      IdAGUserEmailAddController.openDialog(this.configuration, this.strings);
    final var emailOpt = controller.result();
    if (emailOpt.isPresent()) {
      final var future =
        this.client.userEmailAdd(this.user.id(), emailOpt.get());
      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onUserReceived(Optional.of(received));
        }
      });
    }
  }

  private IdAGUserDeleteConfirmController openUserDeleteConfirmDialog()
    throws IOException
  {
    final var stage = new Stage();
    final var connectXML =
      IdAGMainScreenController.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/userDeleteConfirm.fxml");

    final var resources =
      this.strings.resources();
    final var loader =
      new FXMLLoader(connectXML, resources);

    loader.setControllerFactory(
      clazz -> new IdAGUserDeleteConfirmController(
        this.user,
        stage)
    );

    final Pane pane = loader.load();
    IdAGCSS.setCSS(this.configuration, pane);

    final IdAGUserDeleteConfirmController controller = loader.getController();
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setScene(new Scene(pane));
    stage.setTitle(this.strings.format("userDelete.confirmTitle"));
    stage.showAndWait();
    return controller;
  }

  @FXML
  private void onEmailDeleteSelected()
  {
    final var future =
      this.client.userEmailRemove(
        this.user.id(),
        this.userEmailList.getSelectionModel().getSelectedItem()
      );
    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onUserReceived(Optional.of(received));
      }
    });
  }

  @FXML
  private void onUserUpdateSelected()
  {
    final var future =
      this.client.userUpdate(
        this.user.id(),
        Optional.of(new IdName(this.userIdNameField.getText())),
        Optional.of(new IdRealName(this.userRealNameField.getText())),
        Optional.empty()
      );

    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onUserReceived(Optional.of(received));
      }
    });
  }
}
