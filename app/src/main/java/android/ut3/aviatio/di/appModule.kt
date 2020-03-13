package android.ut3.aviatio.di

import android.ut3.aviatio.repository.ScoreRepository
import android.ut3.aviatio.viewmodel.ScoreViewModel
import org.koin.dsl.module


val scoreViewModel = module {
    single {
        ScoreViewModel(get())
    }
}

val scoreRepository = module {
    single {
        ScoreRepository()
    }
}