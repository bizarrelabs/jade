import org.rosuda.JRI.Rengine;

public class RL {

    public static void main(String a[]) {


        // Start Rengine.
        Rengine engine = new Rengine(new String[] { "--no-save" }, false, null);

        // The vector that was created in JAVA context is stored in 'rVector' which is a variable in R context.
        
        //Calculate MEAN of vector using R syntax.
        engine.eval("data<-read.csv(file='Marks.csv',head=TRUE,sep=',')");

        engine.eval("exam_1<-data$exam_1");                
        engine.eval("exam_2<-data$exam_2");                
        engine.eval("admitted<-data$admitted");                
        

        engine.eval("Model_1<-glm(admitted ~ exam_1 +exam_2, family = binomial('logit'), data=data)");                
        engine.eval("summary(Model_1)");
        
        engine.eval("in_frame<-data.frame(exam_1=62,exam_2=58)");

        System.out.println(engine.eval("predict(Model_1,in_frame, type='response')"));

        
        
    }
}
