package util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Muter {

	public static final PrintStream out = System.out;
	public static final PrintStream dummyStream = new PrintStream(new OutputStream() {
		@Override
		public void write(int b) throws IOException {
		}
	});

	public static void mute() {
		System.setOut(dummyStream);
	}

	public static void unmute() {
		System.setOut(out);
	}

	public static <T, R> R callMuted(Function<T, R> func, T arg) {
		mute();
		R res = func.apply(arg);
		unmute();
		return res;
	}

	public static <T, U, R> R callMuted(BiFunction<T, U, R> func, T arg1, U arg2) {
		mute();
		R res = func.apply(arg1, arg2);
		unmute();
		return res;
	}

}
