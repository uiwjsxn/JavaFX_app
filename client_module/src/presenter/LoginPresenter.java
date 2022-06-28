package presenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import fxml_controller.Controller_TypeA;
import fxml_controller.EnterEmailController;
import fxml_controller.LoginController;
import fxml_controller.NetworkSettingController;
import fxml_controller.ResetPasswordController;
import fxml_controller.VerifyEmailController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.ClientProtocol;
import model.NetClient;
import util.DataManager;
import util.Util;

public class LoginPresenter extends Presenter{
	private enum USAGE{LOGIN, REGISTER, RESET_PASSWORD};
	private enum PANE{LOGIN, ENTER_EMAIL, VERIFY_EMAIL, RESET_PASSWORD};
	
	private VBox loginPane;
	private VBox enterEmailPane;
	private VBox verifyEmailPane;
	private VBox resetPasswordPane;
	private VBox networkSettingPane;
	private LoginController loginController;
	private EnterEmailController enterEmailController;
	private VerifyEmailController verifyEmailController;
	private NetworkSettingController networkSettingController;
	private ResetPasswordController resetPasswordController;
	
	private Stage networkSettingStage;
	private ClientProtocol clientProtocol;
	private PANE showingPane = PANE.LOGIN;
	private USAGE currentUsage = USAGE.LOGIN;
	private String emailAddr;
	private Image userImage = null;
	
	public LoginPresenter(ExecutorService threadPool_, Stage stage_, NetClient netClient_, ResourceBundle rb) {
		super(threadPool_, stage_, netClient_, rb);
		try {
			clientProtocol = ClientProtocol.getClientProtocol();
			netClient_.setPresenter(this);
			loadPaneController(new PANE[] {PANE.LOGIN, PANE.ENTER_EMAIL, PANE.VERIFY_EMAIL, PANE.RESET_PASSWORD});
			//get userInfo
			byte[] userInfoBytes = null;
			try(Connection conn = DataManager.getDataManager().getConnection()){
				userInfoBytes = DataManager.getDataManager().getBlobBytesFrom_client_info(conn, "userInfo");
			}catch(SQLException e) {
				e.printStackTrace();
				throw new RuntimeException("Failed to initialize LoginPresenter");
			}
			if(userInfoBytes != null) {
				loadUserData(ByteBuffer.wrap(userInfoBytes));
			}
			//show the first window
			stage.initStyle(StageStyle.UNDECORATED);
			setStage(stage, loginController, loginPane);
			stage.show();
		}catch(Exception e) {
			presenterHandleExceptionExit(e);
			throw new RuntimeException("Failed to load LoginPresenter");
		}
	}
	
	private void disableButton(Button[] buttons) {
		for(Button button : buttons) {
			button.setDisable(true);
		}
	}
	private void enableButton(Button[] buttons) {
		for(Button button : buttons) {
			button.setDisable(false);
		}
	}
	private void setTitleLabel(Label label) {
		switch(currentUsage) {
		case LOGIN:
			label.setText(resourceBundle.getString("Login"));
			break;
		case RESET_PASSWORD:
			label.setText(resourceBundle.getString("Reset_Password"));
			break;
		case REGISTER:
			label.setText(resourceBundle.getString("Register"));
			break;
		}
	}
	
	//fill in the username and password automatically
	private void loadUserData(ByteBuffer byteBuffer) {
		//check whether the user choose to save username and password and show it if necessary
		if(byteBuffer.hasRemaining()) {
			int usernameSize = byteBuffer.getInt();
			byte[] usernameBytes = new byte[usernameSize];
			byteBuffer.get(usernameBytes);
			loginController.usernameTfd.setText(new String(usernameBytes));
			loginController.usernameCB.setSelected(true);
			//get password
			if(byteBuffer.hasRemaining()) {
				byteBuffer.getInt();
				/*//all the bytes remaining in byteBuffer is password bytes, just send the byteBuffer to the Coder and decode it
				ByteBuffer passwordBuffer = Coder.getCoder().userPasswordDecode(byteBuffer);*/
				byte[] passwordBytes = new byte[byteBuffer.remaining()];
				byteBuffer.get(passwordBytes);
				loginController.passwordTfd.setText(new String(passwordBytes));
				loginController.passwordCB.setSelected(true);
			}
		}
	}
	
	private void setTemplateHandler(Controller_TypeA[] controllers) {
		MenuItem menuItemInternet = new MenuItem(resourceBundle.getString("Internet"));
		ToggleGroup toggleGroup = new ToggleGroup();
		RadioMenuItem enMenuItem = new RadioMenuItem("English");
		enMenuItem.setToggleGroup(toggleGroup);
		RadioMenuItem cnMenuItem = new RadioMenuItem("中文");
		cnMenuItem.setToggleGroup(toggleGroup);
		Menu languageMenu = new Menu(resourceBundle.getString("Language"),null,enMenuItem,cnMenuItem);
		
		enMenuItem.setOnAction(event->{
			stage.close();
			ResourceBundle enRb = ResourceBundle.getBundle("languages.language", Locale.ENGLISH);
			setResourceBundle(1,enRb);
			//reset current showing stage
			switch(showingPane) {
			case LOGIN:
				setStage(stage, loginController, loginPane);
				break;
			case ENTER_EMAIL:
				setStage(stage, enterEmailController, enterEmailPane);
				break;
			case VERIFY_EMAIL:
				setStage(stage, verifyEmailController, verifyEmailPane);
				break;
			case RESET_PASSWORD:
				setStage(stage, resetPasswordController, resetPasswordPane);
				break;
			}
			stage.show();
		});
		cnMenuItem.setOnAction(event->{
			stage.close();
			ResourceBundle cnRb = ResourceBundle.getBundle("languages.language", Locale.CHINESE);
			setResourceBundle(2,cnRb);
			stage.show();
		});
		//Internet setting
		menuItemInternet.setOnAction(e->{
			//you must show the stage first, then set the position, or the size of the Scene of networkSettingStage is unknown(zero)
			networkSettingStage.show();
			setStagePosition(networkSettingStage, stage);
		});
		
		ContextMenu contextMenu = new ContextMenu(languageMenu,menuItemInternet);
		
		for(Controller_TypeA controller : controllers) {
			controller.closeBtn.setOnAction(e->onExitingConfirm());
			controller.minusBtn.setOnAction(e->stage.setIconified(true));
			controller.configBtn.setOnAction(e->{
				contextMenu.show(controller.configBtn,Side.BOTTOM,0.0,0.0);
			});
			try {
				Image image = new Image(new File(resourcesDir + File.separator + "pictures" + File.separator + "login.jpg").toURI().toURL().toString(), 600, 150, true, true);
				if(controller.imageView != null) {
					controller.imageView.setImage(image);
				}
			}catch(Exception e) {System.out.println(e.getMessage());}
		}
	}
	//this method should not be called in JavaFX Application Thread, since it will get the thread blocked
	//this method has handle the showing window for connection failure, you should only consider the situation of connection success after calling this method
	private boolean connectToServer() throws Exception{
		boolean isConnected = false;
		try(RandomAccessFile randomAccessFile = new RandomAccessFile(resourcesDir + File.separator + "data" + File.separator + "data3.dat", "rw")){
			try(FileChannel fileChannel = randomAccessFile.getChannel()){
				//the first time you set NetClient
				fileChannel.position(4);
				int dataSize = (int)fileChannel.size()-4;
				if(dataSize == 0) {
					Platform.runLater(()->{
						showPromptStage("Error", resourceBundle.getString("Can_not_build_connection,_no_server_is_set"));
					});
					return false;
				}
				ByteBuffer dataBuffer = ByteBuffer.allocate(dataSize);
				Util.readFullFromFileChannel(fileChannel, dataBuffer, dataBuffer.remaining());
				dataBuffer.flip();
				int hostNumber = dataBuffer.getInt();
				//the host that failed to connect previously
				ArrayList<String> invalidHostnames = new ArrayList<>();
				ArrayList<Integer> invalidPorts = new ArrayList<>();
				//the position of the byte for indicating the validation of the host
				ArrayList<Integer> positions = new ArrayList<>();
				int[] tryedServer = {1};
				
				//Cipher decodeCipher = DataManager.getDataManager().getDecodeCipher();
				while(hostNumber > 0) {
					int pos = dataBuffer.position()+4;
					byte isValid = dataBuffer.get();
					int port = dataBuffer.getInt();
					int hostnameSize = dataBuffer.getInt();
					dataBuffer.limit(dataBuffer.position()+hostnameSize);
					//ByteBuffer hostnameBuffer = Util.decodeBuffer(dataBuffer, decodeCipher);
					ByteBuffer hostnameBuffer = dataBuffer;
					dataBuffer.limit(dataBuffer.capacity());
					String hostname = new String(Util.getAllBytesFromBuffer(hostnameBuffer),"utf-8");
					if(isValid == (byte)0) {
						//firstly connect the host which is valid in previous connection, then try invalid host
						invalidHostnames.add(hostname);
						invalidPorts.add(port);
						positions.add(pos);
					}else {
						netClient.setNetwork(hostname, port);
						if(tryedServer[0] != 1) {
							Platform.runLater(()->{
								showPromptPaneInfo(resourceBundle.getString("Failed,_Connecting_to_another_server..."), String.format("%s %d", resourceBundle.getString("Connecting_to_the_server"), tryedServer[0]++));
							});
						}else {
							Platform.runLater(()->{
								showPromptStage(resourceBundle.getString("Connecting..."), String.format("%s %d", resourceBundle.getString("Connecting_to_the_server"), tryedServer[0]++), aEvent->onExitingConfirm());
							});
						}
						if(netClient.connect()){
							isConnected = true;
							break;
						}else {
							//change the position of fileChannel will be visible to randomAccessFile
							fileChannel.position(pos);
							//connection failed, indicate the host is invalid now
							randomAccessFile.writeByte(0);
						}
					}
					--hostNumber;
				}
				//connection failed after trying all valid hosts, now try invalid hosts
				if(!isConnected) {
					for(int i = 0;i < invalidHostnames.size();++i) {
						if(tryedServer[0] != 1) {
							Platform.runLater(()->{
								showPromptPaneInfo(resourceBundle.getString("Failed,_Connecting_to_another_server..."), String.format("%s %d", resourceBundle.getString("Connecting_to_the_server"), tryedServer[0]++));
							});
						}else {
							Platform.runLater(()->{
								showPromptStage(resourceBundle.getString("Connecting..."), String.format("%s %d", resourceBundle.getString("Connecting_to_the_server"), tryedServer[0]++), aEvent->onExitingConfirm());
							});
						}
						
						netClient.setNetwork(invalidHostnames.get(i), invalidPorts.get(i));
						if(netClient.connect()) {
							isConnected = true;
							//the invalid host assumed is valid now, change the byte
							fileChannel.position(positions.get(i));
							randomAccessFile.writeByte(1);
							Platform.runLater(()->promptStage.close());
							break;
						}
					}
				}else {
					//connected, close promptWindow
					Platform.runLater(()->promptStage.close());
				}
			}catch(Exception e) {throw e;}
		}catch(Exception e){
			throw e;
		}
		if(!isConnected) {
			Platform.runLater(()->{
				showPromptStage(resourceBundle.getString("Connection_Failed"), resourceBundle.getString("Connection_failed_after_trying_all_hosts"),()->loginController.loginBtn.setDisable(false));
			});
		}
		return isConnected;
	}
	//only after pressing login Button, will the TLS connection begin to be built and the data of host name and port begin to be read from data3.dat
	private void setLoginHandler() {
		loginController.loginBtn.setOnAction(event->{
			loginController.loginBtn.setDisable(true);
			String userIDString = loginController.usernameTfd.getText();
			String passwordString = loginController.passwordTfd.getText();
			//userID is a string of number
			if((!userIDString.matches("^\\d+$") && !Util.checkEmailAddressFormat(userIDString)) || passwordString.length() == 0) {
				showPromptStage("", resourceBundle.getString("Wrong_username_or_password"));
				loginController.passwordTfd.clear();
				//loginController.usernameTfd.clear();
			}else {
				threadPool.submit(()->{
					try {
						if(connectToServer()) {
							Platform.runLater(()->showPromptPaneInfo(resourceBundle.getString("Connected"),resourceBundle.getString("Logging_in...")));
							//connection success, sending login buffer
							ByteBuffer loginBuffer = clientProtocol.genLoginBuffer(userIDString, passwordString, loginController.usernameCB.isSelected(), loginController.passwordCB.isSelected());
							//ByteBuffer msgToServer = Coder.getCoder().clientMessageEncode(randomMsgToServer);
							//ByteBuffer lengthBuffer = Util.genIntBuffer(msgToServer.remaining());
							netClient.sendBuffer(new ByteBuffer[] {loginBuffer});
						}
						Platform.runLater(()->loginController.loginBtn.setDisable(false));
					}catch(Exception e) {
						presenterHandleException(e);
					}
				});
			}
		});
		loginController.forgetPasswordBtn.setOnAction(event->{
			loginController.forgetPasswordBtn.setDisable(true);
			threadPool.submit(()->{
				try {
					if(connectToServer()) {
						Platform.runLater(()->{
							stage.close();
							currentUsage = USAGE.RESET_PASSWORD;
							enterEmailController.setUsage(EnterEmailController.USAGE.RESET_PASSWORD);
							setStage(stage, enterEmailController, enterEmailPane);
							setTitleLabel(enterEmailController.titleLabel);
							showingPane = PANE.ENTER_EMAIL;
							stage.show();
						});
					}
					Platform.runLater(()->loginController.forgetPasswordBtn.setDisable(false));
				}catch(Exception e) {
					presenterHandleException(e);
				}
			});
		});
		loginController.registerBtn.setOnAction(event->{
			loginController.registerBtn.setDisable(true);
			threadPool.submit(()->{
				try {
					if(connectToServer()) {
						Platform.runLater(()->{
							stage.close();
							currentUsage = USAGE.REGISTER;
							enterEmailController.setUsage(EnterEmailController.USAGE.REGISTER);
							setStage(stage, enterEmailController, enterEmailPane);
							setTitleLabel(enterEmailController.titleLabel);
							showingPane = PANE.ENTER_EMAIL;
							stage.show();
						});
					}
					Platform.runLater(()->loginController.registerBtn.setDisable(false));
				}catch(Exception e) {
					presenterHandleException(e);
				}
			});
		});
		loginController.usernameTfd.textProperty().addListener((property,oldValue,newValue)->{
			if(!newValue.matches("^\\d+$") && !Util.checkEmailAddressFormat(newValue)) {
				loginController.loginBtn.setDisable(true);
			}else {
				if(loginController.passwordTfd.getText().length() > 0) {
					loginController.loginBtn.setDisable(false);
				}else {
					loginController.loginBtn.setDisable(true);
				}
			}
		});
		loginController.passwordTfd.textProperty().addListener((property,oldValue,newValue)->{
			if(newValue.length() == 0) {
				loginController.loginBtn.setDisable(true);
			}else {
				String userName = loginController.usernameTfd.getText();
				if(userName.matches("^\\d+$") || Util.checkEmailAddressFormat(userName)) {
					loginController.loginBtn.setDisable(false);
				}else {
					loginController.loginBtn.setDisable(true);
				}
			}
		});
		loginController.passwordCB.selectedProperty().addListener((property,oldValue,newValue)->{
			if(newValue) {
				loginController.usernameCB.setSelected(true);
			}
		});
	}

	private void setEnterEmailHandler() {
		enterEmailController.sendEmailBtn.setOnAction(event->{
			enterEmailController.sendEmailBtn.setDisable(true);
			String emailAddress = enterEmailController.emailAddressTfd.getText().strip();
			if(Util.checkEmailAddressFormat(emailAddress)) {
				ByteBuffer emailAddrBuffer = null;
				switch(enterEmailController.getUsage()) {
				case REGISTER:
					emailAddrBuffer = ClientProtocol.getClientProtocol().genEmailAddrBufferForRegister(emailAddress);
					break;
				case RESET_PASSWORD:
					emailAddrBuffer = ClientProtocol.getClientProtocol().genEmailAddrBufferForPasswordReset(emailAddress);
					break;
				default:
					throw new RuntimeException("No such option");
				}
				
				this.emailAddr = emailAddress;
				showPromptStage(resourceBundle.getString("Sending_Email_Address..."), "", aEvent->onExitingConfirm());
				try {
					netClient.sendBuffer(emailAddrBuffer);
				}catch(Exception e){
					presenterHandleException(e);
				}
			}else {
				showPromptStage(resourceBundle.getString("Error"), resourceBundle.getString("Invalid_email_address,_please_enter_it_again"));
			}
			enterEmailController.emailAddressTfd.clear();
		});
		enterEmailController.emailAddressTfd.textProperty().addListener((property, oldValue, newValue)->{
			if(Util.checkEmailAddressFormat(newValue)) {
				enterEmailController.sendEmailBtn.setDisable(false);
			}else {
				enterEmailController.sendEmailBtn.setDisable(true);
			}
		});
		enterEmailController.backBtn.setOnAction(event->{
			if(currentUsage == USAGE.REGISTER) {
				showChoiceStage("Confirm", "Are you sure to cancel the Register", ()->backToLoginStage());
			}else if(currentUsage == USAGE.LOGIN) {
				showChoiceStage("Confirm", "Are you sure to cancel the Login", ()->backToLoginStage());
			}else if(currentUsage == USAGE.RESET_PASSWORD) {
				showChoiceStage("Confirm", "Are you sure to cancel the password reset", ()->backToLoginStage());
			}
		});
	}
	private void setVerifyEmailHandler() {
		verifyEmailController.resendCodeBtn.setOnAction(event->{
			verifyEmailController.resendCodeBtn.setDisable(true);
			threadPool.submit(()->{
				//60 seconds countdown
				final int[] countDown = new int[] {60};
				while((countDown[0]--) > 1) {
					try {
						Thread.sleep(1000);
						Platform.runLater(()->verifyEmailController.countDownLbl.setText(String.valueOf(countDown[0])));
					}catch(InterruptedException e) {}
				}
				Platform.runLater(()->verifyEmailController.resendCodeBtn.setDisable(false));
			});
			ByteBuffer byteBuffer = null;
			//ByteBuffer byteBuffer = ClientProtocol.getClientProtocol().genEmailAddrBuffer(emailAddr);
			switch(enterEmailController.getUsage()) {
			case REGISTER:
				byteBuffer = ClientProtocol.getClientProtocol().genEmailAddrBufferForRegister(emailAddr);
				break;
			case RESET_PASSWORD:
				byteBuffer = ClientProtocol.getClientProtocol().genEmailAddrBufferForPasswordReset(emailAddr);
				break;
			default:
				throw new RuntimeException("No such option");
			}
			try {
				netClient.sendBuffer(byteBuffer);
			}catch(Exception e) {
				presenterHandleException(e);
			}
		});
		verifyEmailController.verifyBtn.setOnAction(event->{
			verifyEmailController.verifyBtn.setDisable(true);
			String codeString = verifyEmailController.codeTfd.getText();
			if(codeString.length() > 0) {
				ByteBuffer byteBuffer = ClientProtocol.getClientProtocol().genSecurityCodeBuffer(codeString);
				ByteBuffer[] buffers = new ByteBuffer[] {byteBuffer};
				/*if(currentUsage == USAGE.LOGIN) {
					//login from new client, send the PublicKey and msgToServer to server
					//ByteBuffer msgToServer = Coder.getCoder().clientMessageEncode(randomMsgToServer);
					//ByteBuffer publicKeyBuffer = Coder.getCoder().getPubKeyBuffer();
					//ByteBuffer pubKeySizeBuffer = ByteBuffer.allocate(4);
					//pubKeySizeBuffer.putInt(publicKeyBuffer.remaining());
					//pubKeySizeBuffer.flip();
					buffers = new ByteBuffer[] {byteBuffer};
				}else {
					buffers = new ByteBuffer[] {byteBuffer};
				}*/
				showPromptStage(resourceBundle.getString("Sending_Security_Code..."), "", aEvent->onExitingConfirm());
				try {
					netClient.sendBuffer(buffers);
				}catch(IOException e) {
					presenterHandleException(e);
				}
			}else {
				showPromptStage(resourceBundle.getString("Error"), resourceBundle.getString("You_have_not_inputted_security_code"));
			}
			verifyEmailController.codeTfd.clear();
		});
		verifyEmailController.codeTfd.focusedProperty().addListener((property, oldValue, newValue)->{
			if(!newValue) {
				if(verifyEmailController.codeTfd.getText().length() > 0) {
					verifyEmailController.verifyBtn.setDisable(false);
				}else {
					verifyEmailController.verifyBtn.setDisable(true);
				}
			}
		});
		verifyEmailController.backBtn.setOnAction(event->{
			if(currentUsage == USAGE.REGISTER) {
				showChoiceStage("Confirm", "Are you sure to cancel the Register", ()->backToLoginStage());
			}else if(currentUsage == USAGE.LOGIN) {
				showChoiceStage("Confirm", "Are you sure to cancel the Login", ()->backToLoginStage());
			}else if(currentUsage == USAGE.RESET_PASSWORD) {
				showChoiceStage("Confirm", "Are you sure to cancel the password reset", ()->backToLoginStage());
			}
		});
		verifyEmailController.codeTfd.textProperty().addListener((oldValue, newValue, property)->{
			if(newValue.length() > 0) {
				verifyEmailController.verifyBtn.setDisable(false);
			}
		});
	}
	
	private void setResetPasswordHandler() {
		resetPasswordController.passwordTfd1.focusedProperty().addListener((property,oldValue,newValue)->{
			if(!newValue) {
				String password1 = resetPasswordController.passwordTfd1.getText();
				String password2 = resetPasswordController.passwordTfd2.getText();
				if(password1.length() > 0 && password1.equals(password2)) {
					resetPasswordController.submitBtn.setDisable(false);
					resetPasswordController.promptLabel.setVisible(false);
				}else {
					resetPasswordController.submitBtn.setDisable(true);
					if(password2.length() > 0) {
						resetPasswordController.promptLabel.setVisible(true);
					}
				}
			}
		});
		resetPasswordController.passwordTfd2.textProperty().addListener((property,oldValue,newValue)->{
			String password1 = resetPasswordController.passwordTfd1.getText();
			//String password2 = resetPasswordController.passwordTfd2.getText();
			if(newValue.length() > 0 && newValue.equals(password1)) {
				resetPasswordController.submitBtn.setDisable(false);
				resetPasswordController.promptLabel.setVisible(false);
			}else {
				resetPasswordController.submitBtn.setDisable(true);
				resetPasswordController.promptLabel.setVisible(true);
			}
		});
		resetPasswordController.cancelBtn.setOnAction(event->{
			if(currentUsage == USAGE.RESET_PASSWORD) {
				showChoiceStage(resourceBundle.getString("Confirm"), resourceBundle.getString("Are_you_sure_to_cancel_password_resetting?"),()->backToLoginStage());
			}else {
				showChoiceStage(resourceBundle.getString("Confirm"), resourceBundle.getString("Are_you_sure_to_cancel_user_register?"),()->backToLoginStage());
			}
		});
		resetPasswordController.submitBtn.setOnAction(event->{
			String password1 = resetPasswordController.passwordTfd1.getText();
			String password2 = resetPasswordController.passwordTfd2.getText();
			
			if(password1.length() > 0 && password1.equals(password2)) {
				System.out.println("resetPasswordController.getImageBytes().length: " + resetPasswordController.getImageBytes().length);
				ByteBuffer buffer = ClientProtocol.getClientProtocol().genNewPasswordBuffer(password1, resetPasswordController.getImageBytes());
				showPromptStage(resourceBundle.getString("Sending..."), resourceBundle.getString("Sending_new_password_to_server"), aEvent->onExitingConfirm());
				try {
					netClient.sendBuffer(buffer);
				}catch(IOException e) {
					presenterHandleException(e);
				}
			}else {
				showPromptStage(resourceBundle.getString("Invalid_Password"), resourceBundle.getString("Password_Requirement_not_satisfied"));
			}
		});
		resetPasswordController.iconCircle.addEventHandler(MouseEvent.MOUSE_CLICKED, aEvent->{
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(resourceBundle.getString("Choose_an_image_file_as_your_profile_picture"));
			fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png", "*.jpg"));
			File file = fileChooser.showOpenDialog(stage);
			if(file != null) {
				try(FileInputStream fileInputStream = new FileInputStream(file)){
					try(FileChannel fileChannel = fileInputStream.getChannel()){
						Image originImage = new Image(fileInputStream);
						Image filledImage = null;
						double width = originImage.getWidth();
						double height = originImage.getHeight();
						if(width > height) {
							//reset the position of FileInputStream by FileChannel, since after calling new Image(fileInputStream), the stream reaches the end
							fileChannel.position(0);
							filledImage = new Image(fileInputStream, 120.0*(width/height), 120.0, true, true);
							fileChannel.position(0);
							//userImage is smaller than filledImage
							userImage = new Image(fileInputStream, 50.0*(width/height), 50.0, true, true);
						}else {
							fileChannel.position(0);
							filledImage = new Image(fileInputStream, width, 120.0*(height/width), true, true);
							fileChannel.position(0);
							userImage = new Image(fileInputStream, width, 50.0*(height/width), true, true);
						}
						resetPasswordController.iconCircle.setFill(new ImagePattern(userImage));
						resetPasswordController.iconPromptLbl.setVisible(false);
						System.out.println("Util.imageToBytes(userImage).length: " + Util.imageToBytes(userImage).length);
						resetPasswordController.setImageBytes(Util.imageToBytes(userImage));
					}catch(IOException e) {throw e;}
				}catch(IOException ee) {
					ee.printStackTrace();
					showPromptStage("Error", "Failed to set profile picture");
				}
			}else {
				//clear the selection if file is null(use does not select any file)
				resetPasswordController.iconCircle.setFill(Color.WHITE);
				resetPasswordController.iconPromptLbl.setVisible(true);
				resetPasswordController.setImageBytes(null);
			}
		});
	}
	//a method for setNetworkSettingHandler()
	private void setServerInfo() {
		File file = new File(resourcesDir + File.separator + "data" + File.separator + "data3.dat");
		if(file.length() > 4L) {
			try(FileInputStream fileInputStream = new FileInputStream(file)) {
				try(FileChannel fileChannel = fileInputStream.getChannel()){
					fileChannel.position(4);
					//write the number of servers
					ByteBuffer numberBuffer = ByteBuffer.allocate(9);
					numberBuffer.limit(4);
					Util.readFullFromFileChannel(fileChannel, numberBuffer, numberBuffer.remaining());
					numberBuffer.flip();
					int size = numberBuffer.getInt();
					String portString, hostnameStr;
					for(int i = 1;i <= 3;++i) {
						if(i <= size) {
							numberBuffer.clear();
							Util.readFullFromFileChannel(fileChannel, numberBuffer, numberBuffer.remaining());
							numberBuffer.flip();
							//skip the first byte, which is "isValid(byte)"
							numberBuffer.position(1);
							int port = numberBuffer.getInt();
							int hostnameSize = numberBuffer.getInt();
							ByteBuffer hostnameBuffer = ByteBuffer.allocate(hostnameSize);
							Util.readFullFromFileChannel(fileChannel, hostnameBuffer, hostnameBuffer.remaining());
							hostnameBuffer.flip();
							//ByteBuffer resBuffer = Util.decodeBuffer(hostnameBuffer, DataManager.getDataManager().getDecodeCipher());
							hostnameStr = new String(Util.getAllBytesFromBuffer(hostnameBuffer),"utf-8");
							portString = String.valueOf(port);
						}else {
							//empty fields
							hostnameStr = "";
							portString = "";
						}
						switch(i) {
						case 1:
							networkSettingController.hostnameTfd1.setText(hostnameStr);
							networkSettingController.portTfd1.setText(portString);
							break;
						case 2:
							networkSettingController.hostnameTfd2.setText(hostnameStr);
							networkSettingController.portTfd2.setText(portString);
							break;
						case 3:
							networkSettingController.hostnameTfd3.setText(hostnameStr);
							networkSettingController.portTfd3.setText(portString);
							break;
						}
					}
				}catch(Exception e) {throw e;}
			}catch(Exception e) {
				e.printStackTrace();
				setPromptStage(resourceBundle.getString("Operation_Failed"), resourceBundle.getString("Failed_to_reset_network"));
				promptStage.showAndWait();
				System.out.println("Fatal Error, Failed to read user setting from data3.dat, which should not happen");
				closePresenter();
			}
		}else {
			//no server info has been documented in data3.dat
			networkSettingController.hostnameTfd1.clear();
			networkSettingController.portTfd1.clear();
			networkSettingController.hostnameTfd2.clear();
			networkSettingController.portTfd2.clear();
			networkSettingController.hostnameTfd3.clear();
			networkSettingController.portTfd3.clear();
		}
	}
	private void setNetworkSettingHandler(Stage networkSettingStage) {
		networkSettingController.confirmBtn.setOnAction(event->{
			disableButton(new Button[] {networkSettingController.confirmBtn,networkSettingController.resetBtn});
			String hostnameStr1 = networkSettingController.hostnameTfd1.getText();
			int port1 = Util.parseUnsignedInteger(networkSettingController.portTfd1.getText());
			String hostnameStr2 = networkSettingController.hostnameTfd2.getText();
			int port2 = Util.parseUnsignedInteger(networkSettingController.portTfd2.getText());
			String hostnameStr3 = networkSettingController.hostnameTfd3.getText();
			int port3 = Util.parseUnsignedInteger(networkSettingController.portTfd3.getText());
			ArrayList<String> validHostnames = new ArrayList<>();
			ArrayList<Integer> validPorts = new ArrayList<>();
			if(networkSettingController.checkHostName(hostnameStr1) && networkSettingController.checkPort(port1)) {
				validHostnames.add(hostnameStr1);
				validPorts.add(port1);
			}else {
				networkSettingController.hostnameTfd1.clear();
				networkSettingController.portTfd1.clear();
			}
			if(networkSettingController.checkHostName(hostnameStr2) && networkSettingController.checkPort(port2)) {
				validHostnames.add(hostnameStr2);
				validPorts.add(port2);
			}else {
				networkSettingController.hostnameTfd2.clear();
				networkSettingController.portTfd2.clear();
			}
			if(networkSettingController.checkHostName(hostnameStr3) && networkSettingController.checkPort(port3)) {
				validHostnames.add(hostnameStr3);
				validPorts.add(port3);
			}else {
				networkSettingController.hostnameTfd3.clear();
				networkSettingController.portTfd3.clear();
			}
			
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(resourceBundle.getString("The_following_servers_have_been_set"));
			stringBuilder.append("\n\n");
			if(validHostnames.isEmpty()) {
				stringBuilder.append(resourceBundle.getString("None,_nothing_changed"));
			}else {
				try(RandomAccessFile output = new RandomAccessFile(new File(resourcesDir + File.separator + "data" + File.separator + "data3.dat"), "rw")){
					try(FileChannel fileChannel = output.getChannel()){
						fileChannel.position(4);
						//write the number of servers
						ByteBuffer numberBuffer = ByteBuffer.allocate(9);
						numberBuffer.putInt(validHostnames.size());
						numberBuffer.flip();
						fileChannel.write(numberBuffer);
						
						//Cipher encodeCipher = DataManager.getDataManager().getEncodeCipher();
						for(int i = 0;i < validHostnames.size();++i) {
							//isValid(byte) + port + hostnameSize + hostnameBytes, this host is just added, assume it is valid
							numberBuffer.clear();
							numberBuffer.put((byte)1);
							numberBuffer.putInt(validPorts.get(i));
							//byte[] encryptedHostname = encodeCipher.doFinal(validHostnames.get(i).getBytes("utf-8"));
							byte[] encryptedHostname = validHostnames.get(i).getBytes("utf-8");
							numberBuffer.putInt(encryptedHostname.length);
							numberBuffer.flip();
							fileChannel.write(new ByteBuffer[] {numberBuffer,ByteBuffer.wrap(encryptedHostname)});
						}
						//after done writing, truncate the file, since the new written data may less than previous data in the file
						fileChannel.truncate(fileChannel.position());
					}catch(Exception e) {throw e;}
					for(int i = 0;i < validHostnames.size();++i) {
						stringBuilder.append(validHostnames.get(i));
						stringBuilder.append(String.format(":%d\n", validPorts.get(i)));
					}
				}catch(Exception e) {
					e.printStackTrace();
					presenterHandleException(new RuntimeException("Failed to write user setting to data3.dat, which should not happen"));
				}
			}
			//close the promptStage and parent stage: networkSettingStage
			showPromptStage(resourceBundle.getString("Operation_Success"), stringBuilder.toString(),()->networkSettingStage.close());
			enableButton(new Button[] {networkSettingController.confirmBtn,networkSettingController.resetBtn});
		});
		
		networkSettingController.resetBtn.setOnAction(event->{
			disableButton(new Button[] {networkSettingController.confirmBtn,networkSettingController.resetBtn});
			setServerInfo();
			enableButton(new Button[] {networkSettingController.confirmBtn,networkSettingController.resetBtn});
		});
		networkSettingController.cancelBtn.setOnAction(event->networkSettingStage.close());
		networkSettingController.closeBtn.setOnAction(event->networkSettingStage.close());
		setServerInfo();
	}
	
	private void reloadPane(){
		loadPaneController(new PANE[] {showingPane});
	}
	//load All the panes used by LoginPresenter
	private void loadPaneController(PANE[] panes) {
		FXMLLoader fxmlLoader;
		try {
			for(PANE pane : panes) {
				switch(pane) {
				case LOGIN:
					fxmlLoader = new FXMLLoader(new File(resourcesDir + File.separator + "fxml" + File.separator + "pane"+ File.separator + "1_login.fxml").toURI().toURL(),resourceBundle);
					loginPane = fxmlLoader.<VBox>load();
					loginController = fxmlLoader.<LoginController>getController();
					setLoginHandler();
					setTemplateHandler(new Controller_TypeA[] {loginController});
					break;
				case ENTER_EMAIL:
					fxmlLoader = new FXMLLoader(new File(resourcesDir + File.separator + "fxml" + File.separator + "pane"+ File.separator + "2_enterEmail.fxml").toURI().toURL(),resourceBundle);
					enterEmailPane = fxmlLoader.<VBox>load();
					enterEmailController = fxmlLoader.<EnterEmailController>getController();
					setEnterEmailHandler();
					setTemplateHandler(new Controller_TypeA[] {enterEmailController});
					break;
				case VERIFY_EMAIL:
					fxmlLoader = new FXMLLoader(new File(resourcesDir + File.separator + "fxml" + File.separator + "pane"+ File.separator + "3_verifyEmail.fxml").toURI().toURL(),resourceBundle);
					verifyEmailPane = fxmlLoader.<VBox>load();
					verifyEmailController = fxmlLoader.<VerifyEmailController>getController();
					setVerifyEmailHandler();
					setTemplateHandler(new Controller_TypeA[] {verifyEmailController});
					break;
				case RESET_PASSWORD:
					fxmlLoader = new FXMLLoader(new File(resourcesDir + File.separator + "fxml" + File.separator + "pane"+ File.separator + "4_resetPassword.fxml").toURI().toURL(),resourceBundle);
					resetPasswordPane = fxmlLoader.<VBox>load();
					resetPasswordController = fxmlLoader.<ResetPasswordController>getController();
					setResetPasswordHandler();
					setTemplateHandler(new Controller_TypeA[] {resetPasswordController});
					break;
				}
			}
			fxmlLoader = new FXMLLoader(new File(resourcesDir + File.separator + "fxml" + File.separator + "common"+ File.separator + "networkSettingWindow.fxml").toURI().toURL(),resourceBundle);
			networkSettingPane = fxmlLoader.<VBox>load();
			networkSettingController = fxmlLoader.<NetworkSettingController>getController();
			networkSettingStage = createNewStage(stage);
			setStage(networkSettingStage, networkSettingController, networkSettingPane);
			setNetworkSettingHandler(networkSettingStage);
		}catch(IOException e) {
			e.printStackTrace();
			presenterHandleExceptionExit(new RuntimeException("Failed to load fxml files in loadPaneController"));
		}
	}
	//go back to initial stage
	void backToLoginStage() {
		//close current stage
		stage.close();
		setStage(stage,loginController,loginPane);
		//close current connection
		netClient.exit();
		showingPane = PANE.LOGIN;
		currentUsage = USAGE.LOGIN;
		stage.show();
	}
	private void enterNewStage(ByteBuffer userDataBuffer) {
		Semaphore semaphore = new Semaphore(0);
		threadPool.submit(()->{
			try {
				//make sure promptStage will be shown for 3 seconds to user
				Thread.sleep(3000);
				semaphore.release();
			}catch(InterruptedException e) {}
		});
		//now all the window(Stage) has been closed
		Stage mainStage = createNewStage();
		//show new MainPresenter Window
		threadPool.submit(()->{
			try {
				semaphore.acquire();	//You should not get JavaFX Application thread blocked at any time!!!
			}catch(InterruptedException e) {}
			Platform.runLater(()->{
				promptStage.close();
				stage.close();
				//start from here !!!
				@SuppressWarnings("unused")
				MainPresenter mainPresenter = new MainPresenter(threadPool, mainStage, netClient, resourceBundle, userDataBuffer, this);
			});
		});
	}
	private void showEnterEmailStage(USAGE usage) {
		//only after the previous stage closed will the new stage be shown by JavaFX Application Thread
		promptStage.close();
		//stage should have been closed
		setStage(stage,enterEmailController, enterEmailPane);
		setTitleLabel(enterEmailController.titleLabel);
		//modify current showing stage;
		showingPane = PANE.ENTER_EMAIL;
		currentUsage = usage;
		stage.show();
	}
	private void showVerifyEmailStage(){
		threadPool.submit(()->{
			//60 seconds countdown
			Platform.runLater(()->verifyEmailController.resendCodeBtn.setDisable(true));
			final int[] countDown = new int[] {60};
			while((countDown[0]--) > 0) {
				try {
					Thread.sleep(1000);
					Platform.runLater(()->verifyEmailController.countDownLbl.setText(String.valueOf(countDown[0])));
				}catch(InterruptedException e) {}
			}
			Platform.runLater(()->verifyEmailController.resendCodeBtn.setDisable(false));
		});
		promptStage.close();
		setStage(stage, verifyEmailController, verifyEmailPane);
		setTitleLabel(verifyEmailController.titleLabel);
		showingPane = PANE.VERIFY_EMAIL;
		stage.show();
	}
	private void showResetPasswordStage() {
		promptStage.close();
		setStage(stage, resetPasswordController, resetPasswordPane);
		setTitleLabel(resetPasswordController.titleLabel);
		showingPane = PANE.RESET_PASSWORD;
		stage.show();
	}
/******************************************data receiving methods******************************************/
//methods for receiving data from server
	private void handleLoginResult(ByteBuffer byteBuffer) {
		int res = byteBuffer.getInt();
		switch(res) {
		case 0:
			showPromptStage(resourceBundle.getString("Login_Failed"), resourceBundle.getString("Wrong_username_or_password"));
			break;
		case 1:
			//there are more window to be shown
			//setPromptStage("", "",event->onExitingConfirm());
			//stage.close();
			showPromptStage(resourceBundle.getString("Login_Success"), resourceBundle.getString("Login_Success"));
			//loading more data containing userData from bytebuffer
			enterNewStage(byteBuffer);
			break;
		default:
			presenterHandleExceptionExit(new RuntimeException("Fatal Error, unrecognized protocol"));
		}
	}
	private void handleEmailAddressResult(ByteBuffer byteBuffer) {
		int res = byteBuffer.getInt();
		Semaphore semaphore = new Semaphore(0);
		switch(res) {
		case 0:
			showPromptStage(resourceBundle.getString("Failed_to_Send_Security_Code._Check_your_email"), resourceBundle.getString("Invalid_email_address"),()->backToLoginStage());
			break;
		case 1:
			showPromptStage(resourceBundle.getString("Security_Code_Sent"), "", event->onExitingConfirm());
			promptStage.show();
			threadPool.submit(()->{
				try {
					Thread.sleep(1500);
					semaphore.release();
				}catch(InterruptedException e) {}
			});
			threadPool.submit(()->{
				try {
					semaphore.acquire();
				}catch(InterruptedException e) {}
				Platform.runLater(()->showVerifyEmailStage());
			});
			break;
		case 2:
			showPromptStage(resourceBundle.getString("Your_operation_is_too_frequent._Try_again_after_one_minute"), resourceBundle.getString("Invalid_email_address"));
			break;
		default:
			presenterHandleExceptionExit(new RuntimeException("Protocol Unrecognized by server"));
		}
	}
	private void handleSecurityCode(ByteBuffer byteBuffer) {
		int res = byteBuffer.getInt();
		Semaphore semaphore = new Semaphore(0);
		//close current Stage
		stage.close();
		switch(res) {
		case 0:
			showPromptStage(resourceBundle.getString("Failure"), resourceBundle.getString("Failed_to_verify_your_email"),()->backToLoginStage());
			break;
		case 1:
			showPromptStage(resourceBundle.getString("Success"), resourceBundle.getString("Your_email_has_been_verified"),()->onExitingConfirm());
			threadPool.submit(()->{
				try {
					Thread.sleep(1500);
					semaphore.release();
				}catch(InterruptedException e) {}
			});
			threadPool.submit(()->{
				try {
					semaphore.acquire();
				}catch(InterruptedException e) {}
				Platform.runLater(()->showResetPasswordStage());
			});
			break;
		default:
			presenterHandleExceptionExit(new RuntimeException("Fatal Error, unrecognized protocol"));
		}
	}
	private void handleNewPasswordResult(ByteBuffer byteBuffer) {
		int res = byteBuffer.getInt();
		setPromptStage("", "",()->backToLoginStage());
		Semaphore semaphore = new Semaphore(0);
		switch(res) {
		case 0:
			showPromptPaneInfo(resourceBundle.getString("Failure"), resourceBundle.getString("Failed_to_set_your_password_now,_try_agin_later."));
			break;
		case 1:
			if(enterEmailController.getUsage() == EnterEmailController.USAGE.RESET_PASSWORD) {
				showPromptPaneInfo(resourceBundle.getString("Success"), resourceBundle.getString("Your_password_has_been_reset."));
			}else {
				showPromptPaneInfo(resourceBundle.getString("Success"), resourceBundle.getString("Your_account_has_been_created."));
			}
			threadPool.submit(()->{
				try {
					Thread.sleep(1500);
					semaphore.release();
				}catch(InterruptedException e) {}
			});
			break;
		default:
			presenterHandleExceptionExit(new RuntimeException("Fatal Error, unrecognized protocol"));
		}
		threadPool.submit(()->{
			try {
				semaphore.acquire();
				Platform.runLater(()->{
					promptStage.close();
					backToLoginStage();
				});
			}catch(InterruptedException e) {}
		});
	}
/******************************************public methods******************************************/
//all the public methods below should be run in JavaFX Application Thread
	public void setResourceBundle(int languageCode, ResourceBundle rb) {
		/*super.setResourceBundle(languageCode,rb);
		reloadPane();*/
	}
	//receiving server messages, 
	@Override public void receiveData(ByteBuffer byteBuffer, int bufferIndex) {
		int protocolID = byteBuffer.getInt();
		switch(protocolID) {
		//login has no file transmission, therefore no case 1
		//no case 2, since it is handled by NetClient
		case 3:
			handleLoginResult(byteBuffer);
			netClient.giveBackBuffer(bufferIndex);
			break;
		case 4:
		case 5:
		case 6:
			handleEmailAddressResult(byteBuffer);
			netClient.giveBackBuffer(bufferIndex);
			break;
		case 7:
			handleSecurityCode(byteBuffer);
			netClient.giveBackBuffer(bufferIndex);
			break;
		case 8:
			handleNewPasswordResult(byteBuffer);
			netClient.giveBackBuffer(bufferIndex);
			break;
		default:
			presenterHandleExceptionExit(new RuntimeException("Fatal Error, unrecognized protocol"));
		}
	}
	//when the connection to server breaks, go back to NeedLoginState
		//all the public methods called by netClient should be executed in JavaFX Application Thread(Application.runLatar(...))
	@Override public void reconnect() {
			setPromptStage("Connection Failed", "Connection failed");
			Semaphore semaphore = new Semaphore(0);
			threadPool.submit(()->{
				try {
					//make sure promptStage will be shown for 3 seconds to user
					Thread.sleep(1500);
					semaphore.release();
				}catch(InterruptedException e) {}
			});
			threadPool.submit(()->{
				try {
					semaphore.acquire();
				}catch(InterruptedException e) {}
				Platform.runLater(()->backToLoginStage());
			});
		}
}
