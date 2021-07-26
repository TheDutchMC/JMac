package dev.array21.test;

int x(){ 
    return 1;
}

macro_rules! handleA {
    ($a:expr) => {
        if($a.equals("a")) {
            return 0;
        }

        if($a.equals("b")) {
            return 1;
        }
    }
}

int y(){ 
    return 1;
}
