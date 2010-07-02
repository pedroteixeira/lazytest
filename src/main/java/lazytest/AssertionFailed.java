package lazytest;

public class AssertionFailed extends Error {
    public final Object detail;
    
    public AssertionFailed() {
	this.detail = null;
    }

    public AssertionFailed(Object detail) {
	this.detail = detail;
    }
}
