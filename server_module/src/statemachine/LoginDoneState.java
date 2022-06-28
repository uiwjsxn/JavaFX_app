package statemachine;

class LoginDoneState extends State{
	private StateMachine stateMachine;
	
	public LoginDoneState(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
}
